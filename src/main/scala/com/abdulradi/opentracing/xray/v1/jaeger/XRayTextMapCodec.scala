package com.abdulradi.opentracing.xray.v1.jaeger

import scala.util.Random

import com.abdulradi.opentracing.xray.v1.model.TracingHeader
import com.uber.jaeger.SpanContext
import com.uber.jaeger.propagation.{Extractor, Injector}
import io.opentracing.propagation.TextMap
import com.uber.jaeger.samplers.Sampler
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11

final case class OpenTracingXRayException(msg: String) extends Exception(msg)

class XRayTextMapCodec(sampler: Sampler) extends Injector[TextMap] with Extractor[TextMap] {
  import ConversionOps._
  import XRayTextMapCodec._

  override def inject(spanContext: SpanContext, carrier: TextMap): Unit =
    (carrier.put _).tupled(
      TracingHeader
        .fromSpanContext(spanContext)
        .map(_.toHeader)
        .orFail
    )

  override def extract(carrier: TextMap): SpanContext =
    TracingHeader
      .fromHeadersOrCreateNew(carrier)
      .map(spanContextFromTracingHeader(_, sampler, Random.nextLong()))
      .orFail
}

object XRayTextMapCodec {
  implicit class EitherOps[T](val underlying: Either[String, T]) extends AnyVal {
    def orFail()(implicit ev: Null <:< T): T =
      underlying.fold(e => throw OpenTracingXRayException(e), identity)
  }
}