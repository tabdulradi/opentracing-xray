package com.abdulradi.opentracing.xray.v1.jaeger

import com.abdulradi.opentracing.xray.v1.Format._
import com.abdulradi.opentracing.xray.v1.jaeger.senders.AgentBased
import com.uber.jaeger.Tracer
import com.uber.jaeger.metrics.Metrics
import com.uber.jaeger.reporters.RemoteReporter
import com.uber.jaeger.samplers.Sampler
import io.opentracing.propagation.Format

object TracerBuilder {
  def apply(
    serviceName: String,
    agentHost: String,
    agentPort: Int,
    flushInterval: Int,
    maxQueueSize: Int,
    metrics: Metrics,
    sampler: Sampler
  ): Tracer.Builder = {
    val codec = new XRayTextMapCodec(sampler)
    val reporter = AgentBasedXRayReporter(agentHost, agentPort, flushInterval, maxQueueSize, metrics)
    new Tracer.Builder(serviceName, reporter, sampler)
      .registerInjector(Format.Builtin.HTTP_HEADERS, codec)
      .registerExtractor(Format.Builtin.HTTP_HEADERS, codec)
      .registerInjector(Format.Builtin.TEXT_MAP, codec)
      .registerExtractor(Format.Builtin.TEXT_MAP, codec)
  }
}

object AgentBasedXRayReporter {
  def apply(agentHost: String, agentPort: Int, flushInterval: Int, maxQueueSize: Int, metrics: Metrics) =
    new RemoteReporter(
      AgentBased(agentHost, agentPort, SpanConverter),
      flushInterval,
      maxQueueSize,
      metrics
    )
}
