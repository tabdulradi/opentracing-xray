package com.abdulradi.opentracing.xray.tracing

import java.util.Map.Entry

import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model.{SegmentId, TraceId, TracingHeader}
import scala.collection.JavaConverters._
import scala.util.Random

import com.abdulradi.opentracing.xray.utils.RichMap._
import eu.timepit.refined.refineV
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11

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

    private[tracing] def generate(): TraceId =
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
}