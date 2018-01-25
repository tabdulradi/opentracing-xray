package com.abdulradi.opentracing.xray.v1.zipkin

import java.lang.Boolean
import java.util

import cats.syntax.either._
import brave.Tracing
import brave.opentracing.BraveTracer
import brave.propagation.Propagation.{Getter, KeyFactory, Setter}
import brave.propagation.TraceContext.{Extractor, Injector}
import brave.propagation.aws.AWSPropagation
import brave.propagation.{Propagation, TraceContext, TraceContextOrSamplingFlags}
import brave.sampler.Sampler
import com.abdulradi.opentracing.xray
import com.abdulradi.opentracing.xray.v1.senders.AgentBasedSender
import zipkin2.reporter.Reporter
import zipkin2.{Endpoint, Span}

object TracerBuilder {
  def apply(serviceName: String, agentHost: String, agentPort: Int, sampler: Sampler): BraveTracer.Builder =
    BraveTracer.newBuilder(
      Tracing
        .newBuilder()
        .localEndpoint(
          Endpoint
            .newBuilder()
            .serviceName(serviceName)
            .build()
        )
        .propagationFactory(new XRayPropagationFactory(sampler))
        .sampler(sampler)
        .spanReporter(new XRayAgentBasedReporter(agentHost, agentPort))
        .supportsJoin(false)
        .traceId128Bit(true)
        .build()
    )
}

class XRayPropagationFactory(sampler: Sampler) extends Propagation.Factory {
  val underlying = AWSPropagation.FACTORY

  override def create[K](keyFactory: KeyFactory[K]): Propagation[K] =
    new XRayPropagation(keyFactory, underlying.create(keyFactory), sampler)
}

class XRayPropagation[K](keyFactory: KeyFactory[K], underlying: Propagation[K], sampler: Sampler) extends Propagation[K] {
  override def injector[C](setter: Setter[C, K]): Injector[C] =
    new XRayInjector(underlying.injector[C] _, setter)

  override def keys(): util.List[K] =
    underlying.keys()

  override def extractor[C](getter: Getter[C, K]): Extractor[C] =
    new XRayExtractor(getter, keyFactory, sampler)
}

class ManglingSetter[C, K](underlying: Setter[C, K]) extends Setter[C, K] {
  override def put(carrier: C, key: K, value: String): Unit = {
    underlying.put(carrier, key, value.replaceFirst(";Parent=0000000000000000", ""))
  }
}

class XRayInjector[C, K](underlying: Setter[C, K] => Injector[C], setter: Setter[C, K]) extends Injector[C] {
  override def inject(ctx: TraceContext, carrier: C): Unit =
    underlying(if (ctx.parentId == null || ctx.parentId == 0L) new ManglingSetter(setter) else setter)
      .inject(ctx, carrier)
}

class XRayExtractor[C, K](getter: Getter[C, K], keyFactory: KeyFactory[K], sampler: Sampler) extends Extractor[C] {
  override def extract(carrier: C): TraceContextOrSamplingFlags = {
    val header =
      xray.tracing.Propagation.parseOrGenerate(keyName => Option(getter.get(carrier, keyFactory.create(keyName))))

    val (traceIdHigh, traceId) = {
      import java.lang.Long.parseUnsignedLong
      val (high, low) = (header.rootTraceId.originalRequestTime.value + header.rootTraceId.identifier.value).splitAt(16)
      (parseUnsignedLong(high, 16), parseUnsignedLong(low, 16))
    }

    val parentId = header.parentSegmentId.map(p => java.lang.Long.parseUnsignedLong(p.value, 16))
    val samplingDecision = header.samplingDecision.getOrElse(sampler.isSampled(traceId))

    TraceContextOrSamplingFlags.create(
      TraceContext
        .newBuilder
        .traceIdHigh(traceIdHigh)
        .traceId(traceId)
        .spanId(parentId.getOrElse(0)) // 0 is a special dummy value, filtered out by SpanConverter
        .shared(false) // Spans are never shared across multiple hosts, simply because tracing header doesn't include spanId
        .sampled(samplingDecision)
//        .extra(java.util.Collections.singletonList(new AWSPropagation.Extra)) // TODO: AWSPropagation.Extra is private
        .build
    )
  }
}

class XRayAgentBasedReporter(agentHost: String, agentPort: Int) extends Reporter[Span] {
  private val sender = AgentBasedSender(agentHost, agentPort) // FIXME: Api doesn't allow closing the socket
  import com.abdulradi.opentracing.xray.v1.Format._

  override def report(span: Span): Unit =
    SpanConverter(span).map(t => sender.send(t))
}
