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

  private val sampler = new ConstSampler(true) // TODO
  private val codec = new XRayTextMapCodec(sampler)

  def apply(config: Configuration): Tracer =
    new Tracer.Builder(
      config.serviceName,
      new RemoteReporter(
        AgentBased(config.agentAddress, config.agentPort, SpanConverter),
        config.flushInterval,
        config.maxQueueSize,
        new Metrics(new StatsFactoryImpl(new InMemoryStatsReporter)) // TODO
      ),
      sampler
    )
      .registerInjector(Format.Builtin.HTTP_HEADERS, codec)
      .registerExtractor(Format.Builtin.HTTP_HEADERS, codec)
      .registerInjector(Format.Builtin.TEXT_MAP, codec)
      .registerExtractor(Format.Builtin.TEXT_MAP, codec)
      .build()
}
