package com.abdulradi.opentracing.xray.v1.jaeger

import com.abdulradi.opentracing.xray.v1.jaeger.senders.AgentBased
import com.uber.jaeger.Tracer
import com.uber.jaeger.metrics.{InMemoryStatsReporter, Metrics, StatsFactoryImpl}
import com.uber.jaeger.reporters.RemoteReporter
import com.uber.jaeger.samplers.ConstSampler
import com.abdulradi.opentracing.xray.v1.Format._
import io.opentracing.propagation.Format

object TracerFactory {
  final case class Configuration(
    serviceName: String,
    flushInterval: Int,
    maxQueueSize: Int,
    agentAddress: String,
    agentPort: Int
  )

  def apply(config: Configuration): Tracer =
    new Tracer.Builder(
      config.serviceName,
      new RemoteReporter(
        AgentBased(config.agentAddress, config.agentPort, SpanConverter),
        config.flushInterval,
        config.maxQueueSize,
        new Metrics(new StatsFactoryImpl(new InMemoryStatsReporter)) // TODO
      ),
      new ConstSampler(true) // TODO
    )
      .registerInjector(Format.Builtin.HTTP_HEADERS, XRayTextMapCodec)
      .registerExtractor(Format.Builtin.HTTP_HEADERS, XRayTextMapCodec)
      .registerInjector(Format.Builtin.TEXT_MAP, XRayTextMapCodec)
      .registerExtractor(Format.Builtin.TEXT_MAP, XRayTextMapCodec)
      .build()
}
