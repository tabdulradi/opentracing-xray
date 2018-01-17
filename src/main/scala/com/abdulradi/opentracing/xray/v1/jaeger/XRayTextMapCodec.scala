package com.abdulradi.opentracing.xray.v1.jaeger

import scala.util.Random

import com.abdulradi.opentracing.xray.v1.model.TracingHeader
import com.uber.jaeger.SpanContext
import com.uber.jaeger.propagation.{Extractor, Injector}
import io.opentracing.propagation.TextMap
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11

final case class OpenTracingXRayException(msg: String) extends Exception(msg)

object XRayTextMapCodec extends Injector[TextMap] with Extractor[TextMap] {
  import ConversionOps._

  override def inject(spanContext: SpanContext, carrier: TextMap): Unit =
    (carrier.put _).tupled(
      TracingHeader
        .fromSpanContext(spanContext)
        .doubleMap(_.toHeader)
        .horribleGet
    )

  override def extract(carrier: TextMap): SpanContext =
    TracingHeader
      .fromHeaders(carrier)
      .doubleMap(spanContextFromTracingHeader(_, Random.nextLong()))
      .horribleGet


  implicit class EitherOfOptionOps[T](val underlying: Either[String, Option[T]]) extends AnyVal {
    // Throws exceptions, returns null, and does all the horrible things for you!
    def horribleGet()(implicit ev: Null <:< T): T =
      underlying
        .fold(e => throw OpenTracingXRayException(e), identity)
        .orNull

    // Poor man's monad transformer
    def doubleMap[U](f: T => U): Either[String, Option[U]] = underlying.map(_.map(f))
  }
}
