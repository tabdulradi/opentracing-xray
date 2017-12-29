package com.abdulradi.opentracing.xray.v1.jaeger

import scala.util.Random

import com.abdulradi.opentracing.xray.v1.model.TracingHeader
import com.uber.jaeger.SpanContext
import com.uber.jaeger.propagation.{Extractor, Injector}
import io.opentracing.propagation.TextMap

object XRayTextMapCodec extends Injector[TextMap] with Extractor[TextMap] {
  import ConversionOps._

  override def inject(spanContext: SpanContext, carrier: TextMap): Unit =
    (carrier.put _).tupled(
      TracingHeader
        .fromSpanContext(spanContext)
        .map(_.toHeader)
        .fold(e => throw new IllegalStateException(e), identity)
    )

  override def extract(carrier: TextMap): SpanContext =
    TracingHeader
      .fromHeaders(carrier)
      .map(_.map(tracingHeader => spanContextFromTracingHeader(tracingHeader, Random.nextLong())))
      .fold(e => throw new IllegalStateException(e), identity)
      .orNull
}
