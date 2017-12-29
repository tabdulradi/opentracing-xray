package com.abdulradi.opentracing.xray.v1.jaeger

import com.abdulradi.opentracing.xray.v1.jaeger.senders.AgentBased
import com.abdulradi.opentracing.xray.v1.model.TopLevelTrace
import com.uber.jaeger.Tracer
import com.uber.jaeger.metrics.{InMemoryStatsReporter, Metrics, StatsFactoryImpl}
import com.uber.jaeger.reporters.RemoteReporter
import com.uber.jaeger.samplers.ConstSampler
import io.circe.Encoder
import io.opentracing.propagation.Format

final case class TracerConfiguration(
  serviceName: String,
  flushInterval: Int,
  maxQueueSize: Int,
  agentAddress: String,
  agentPort: Int
)

object TracerFactory {

  implicit val encoder: Encoder[TopLevelTrace] = ??? // TODO: WIP

  def apply(config: TracerConfiguration): Tracer =
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
