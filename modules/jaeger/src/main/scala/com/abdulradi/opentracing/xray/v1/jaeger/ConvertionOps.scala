package com.abdulradi.opentracing.xray.v1.jaeger

import java.util.Map.Entry

import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model.{SegmentId, TraceId, TracingHeader}
import com.abdulradi.opentracing.xray.utils.RichOptionOfEither._
import scala.collection.JavaConverters._
import scala.util.Random

import com.abdulradi.opentracing.xray.utils.RichMap._
import com.uber.jaeger.{Span, SpanContext}
import eu.timepit.refined.refineV
import io.opentracing.propagation.TextMap
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11
import com.abdulradi.opentracing.xray.tracing.Propagation
import com.uber.jaeger.samplers.Sampler

object ConversionOps {

  /* ********************************************
   * Segment Id
   *********************************************/
  implicit class SegmentIdConversionOps(val underlying: SegmentId) {
    private[ConversionOps] def toOpenTracing = java.lang.Long.parseUnsignedLong(underlying.value, 16)
  }

  implicit class OptionalSegmentIdConversionOps(val underlying: Option[SegmentId]) extends AnyVal {
    private[ConversionOps] def toOpenTracing: Long = underlying.map(_.toOpenTracing).getOrElse(0)
  }

  implicit class SegmentIdCompanionConversionOps(val underlying: SegmentId.type) extends AnyVal {
    def fromOpenTracing(value: Long): SegmentId = {
      val traceIdHex = value.toHexString
      val padding = "0" * (16 - traceIdHex.length)
      refineV[Hex.P16].unsafeFrom(padding + traceIdHex) // trust me, computer.
    }

    def fromOptional(value: Long): Option[SegmentId] =
      Some(value).filter(_ != 0).map(fromOpenTracing)
  }


  /* ********************************************
   * Trace Id
   *********************************************/
  implicit class TraceIdConversionOps(val underlying: TraceId) extends AnyVal {
    private[ConversionOps] def toBaggage: Map[String, String] = Map(
      BaggageKeys.OriginalRequestTimestamp -> underlying.originalRequestTime.value,
      BaggageKeys.TracingIdentifier -> underlying.identifier.value
    )
  }

  implicit class TraceIdCompanionConversionOps(val underlying: TraceId.type) extends AnyVal {
    def getOrCreate(span: Span): Either[String, TraceId] =
      for {
        maybeExistingTraceId <- fromBaggage(span.context.baggageItems)
        traceId <- maybeExistingTraceId.map(_.asRight).getOrElse(
          if (span.context.hasDummyTraceId)
            Left("TraceId set to dummy value, but no traceId nor timestamp in baggage")
          else
            Right(create(span.context.getTraceId, span.getStart / 1000000)) // microseconds to seconds
        )
      } yield traceId

    private[this] def create(openTracingTraceId: Long, originalRequestTimeSeconds: Long): TraceId = {
      val traceId = {
        val hex = openTracingTraceId.toHexString
        val padding = "0" * (24 - hex.length)
        refineV[Hex.P24].unsafeFrom(padding + hex)
      }
      val timestamp = {
        val hex = originalRequestTimeSeconds.toHexString
        val padding = "0" * (8 - hex.length) // kinda useless
        // This is fine till 02/07/2106, after that timestamps won't fit XRay v1 model anyway.
        refineV[Hex.P8].unsafeFrom(padding + hex)
      }
      TraceId(timestamp, traceId)
    }

    private[ConversionOps] def generate(): TraceId =
      create(
        Random.nextLong(), // -ve is fine, .toHexString treats as unsigned (i.e overflows to fxxxxxx)
        System.currentTimeMillis() / 1000 // millis to seconds
      )

    private[ConversionOps] def fromBaggage(jBaggage: java.lang.Iterable[Entry[String, String]]): Either[String, Option[TraceId]] = {
      val baggage = jBaggage.asScala.to[List]
      val mayebRawOriginalRequestTimestamp = baggage.find(BaggageKeys.OriginalRequestTimestamp)
      val mayebRawTracingIdentifier = baggage.find(BaggageKeys.TracingIdentifier)
      (mayebRawOriginalRequestTimestamp, mayebRawTracingIdentifier) match {
        case (Some(rawOriginalRequestTimestamp), Some(rawTracingIdentifier)) =>
          for {
            originalRequestTimestamp <- refineV[Hex.P8](rawOriginalRequestTimestamp)
            tracingIdentifier <- refineV[Hex.P24](rawTracingIdentifier)
          } yield Some(TraceId(originalRequestTimestamp, tracingIdentifier))
        case (None, None) => Right(None)
        case (Some(a), None) => Left(s"Tracing Identifier is missing (timestamp was $a)")
        case (None, Some(b)) => Left(s"OriginalRequestTimestamp missing (Tracing Id was $b)")
      }
    }
  }

  private object BaggageKeys {
    private val Prefix = "openxraytracer"
    val OriginalRequestTimestamp = s"$Prefix.rootTraceId.originalRequestTimestamp"
    val TracingIdentifier = s"$Prefix.rootTraceId.tracingIdentifier"
  }


  /* ********************************************
   * Tracing Header
   *********************************************/
  implicit class TracingHeaderConversionOps(val underlying: TracingHeader) extends AnyVal {
    import com.abdulradi.opentracing.xray.v1.CommonOps._

    private[this] def toHeaderString: String =
      Map(
        TracingHeader.Keys.Root -> Some(underlying.rootTraceId.toHeaderString),
        TracingHeader.Keys.Parent -> underlying.parentSegmentId.map(_.value),
        TracingHeader.Keys.Sampled -> underlying.samplingDecision
      ).collect { case (key, Some(value)) => s"$key=$value" }.mkString(";")

    def toHeader: (String, String) =
      TracingHeader.Keys.HttpHeaderKey -> toHeaderString
  }

  implicit class TracingHeaderCompanionConversionOps(val underlying: TracingHeader.type) extends AnyVal {
    private[this] def fromHeaders(headers: TextMap): Either[String, Option[TracingHeader]] =
      headers.iterator.asScala
        .find(entry => TracingHeader.Keys.HttpHeaderKey.equalsIgnoreCase(entry.getKey))
        .map(entry => Propagation.parseHeaderValue(entry.getValue))
        .toEitherOfOption

    // This method is a workaround a limitation ...
    // Scenario: A request has been received without a tracing header,
    // this means we are running in a user facing service, with no Load balancer if front of it
    // (or a behind a service that doesn't propagate tracing headers)
    // Ideally we should report that there is no TracingHeader found, which is communicated as a `null` to Jaeger
    // but this won't stop Jaeger from sampling the request, assigning a random opentracing traceId (Long)
    // this will bite us back once the service starts calling other service, expecting us to generate a trace header for propagation
    // return a None/null in this case yeild an error, Jaeger expects us to be able to generate a tracing header from only a trace context
    // which is impossible, because XRay traceId is bigger that open tracing traceId, it includes extra fields like Initial-request-timestamp
    // If choose to lie about timestamp (by generating one), it will be different every time, even if 2 requests are related and should have the exact same traceId
    // So instead of going down this rabbit hole, we instead lie about tracing header, if we didn't fine one, we will create one ourselves
    // now we can generate a timestamp once, then refer it to it multiple time, ensuring consistency,
    def fromHeadersOrCreateNew(headers: TextMap): Either[String, TracingHeader] =
      fromHeaders(headers).map(_.getOrElse(TracingHeader(TraceId.generate(), None, None, Map.empty)))

    def fromSpanContext(spanContext: SpanContext): Either[String, TracingHeader] =
      for {
        maybeTraceIdFromBaggage <- TraceId.fromBaggage(spanContext.baggageItems())
        // If no traceId in baggage, this it is a bug, see docs of `fromHeadersOrCreateNew` for context
        traceId <- maybeTraceIdFromBaggage.fold(s"No TraceId found in baggage".asLeft[TraceId])(_.asRight)
        parentSegmentId = SegmentId.fromOptional(spanContext.getParentId)
        samplingDecision = Option(spanContext.isSampled)
      } yield TracingHeader(traceId, parentSegmentId, samplingDecision, Map.empty)
  }

  /* ********************************************
   * Span Context
   *********************************************/
  // Dummy values that indicated that real values are stored in Baggage instead. since Jaeger's types won't fit the value
  private[this] val DummyTraceId: Long = 0

  implicit class SpanContextConversionOps(val underlying: SpanContext) extends AnyVal {
    def hasDummyTraceId: Boolean =
      underlying.getTraceId == DummyTraceId
  }

  def spanContextFromTracingHeader(tracingHeader: TracingHeader, sampler: Sampler, newSpanId: Long): SpanContext =
    tracingHeader.rootTraceId.toBaggage.foldLeft(new SpanContext(
      DummyTraceId, // rootTraceId won't fit here, instead it is stored in Baggage
      newSpanId,
      tracingHeader.parentSegmentId.toOpenTracing,
      tracingHeader
        .samplingDecision
        .map(sampled => if (sampled) 1 else 0)
        .getOrElse(if (sampler.sample("", newSpanId).isSampled) 1 else 0) // Samples if decision is not specified in headers
        .toByte
    )) { case (spanContext, (key, value)) => spanContext.withBaggageItem(key, value) }

  private[this] object Parsers {
    import atto._
    import Atto._
    import Parser.{Failure, State, Success, TResult}
    import eu.timepit.refined.api.{Refined, Validate}

    type Result[T] = Either[String, T]

    implicit class RefinedOps[T](val parser: Parser[T]) extends AnyVal {

      import eu.timepit.refined.refineV

      def refined[P](implicit ev: Validate[T, P]): Parser[T Refined P] = {
        parser.flatMap(raw => new Parser[T Refined P] {
          override def apply[R](st0: State, kf: Failure[R], ks: Success[Refined[T, P], R]): TResult[R] =
            refineV(raw).fold(
              e => kf(st0, List.empty, e), // FIXME: is empty stack correct here?
              v => ks(st0, v)
            )
        })
      }
    }

    private val hexStr = stringOf(hexDigit)
    private val hex8 = hexStr.refined[Hex.P8]
    private val hex16 = hexStr.refined[Hex.P16]
    private val hex24 = hexStr.refined[Hex.P24]

    private val headerData = {
      val keyValuePair = pairBy(stringOf1(notChar('=')), char('='), stringOf1(notChar(';')))
      sepBy(keyValuePair, char(';')).map(_.toMap)
    }

    private val traceId: Parser[TraceId] =
      string("1-") ~> pairBy(hex8, char('-'), hex24).map(TraceId.tupled)

    private def parse[T](parser: Parser[T])(input: String): Result[T] = parser.parse(input).done.either

    val parseTracingHeader = parse(headerData) _
    val parseTraceId = parse(traceId) _
    val parseSegmentId = parse[SegmentId](hex16) _
    val parseSamplingDecision = parse(int) _
  }
}