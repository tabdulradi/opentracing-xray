package com.abdulradi.opentracing.xray.v1.zipkin

import brave.Tracing
import brave.propagation.aws.AWSPropagation
import brave.sampler.Sampler
import com.abdulradi.opentracing.xray.v1.senders.AgentBasedSender
import zipkin2.reporter.Reporter
import zipkin2.{Endpoint, Span}

object TracerBuilder {
  def apply(
     serviceName: String,
     agentHost: String,
     agentPort: Int,
     sampler: Sampler
   ): Tracing.Builder =
    Tracing
      .newBuilder()
      .localEndpoint(
        Endpoint
          .newBuilder()
          .serviceName(serviceName)
          .build()
      )
      .propagationFactory(AWSPropagation.FACTORY)
      .sampler(sampler)
      .spanReporter(new XRayAgentBasedReporter(agentHost, agentPort))
      .supportsJoin(false)
      .traceId128Bit(true)
}

class XRayAgentBasedReporter(agentHost: String, agentPort: Int) extends Reporter[Span] {
  private val sender = AgentBasedSender(agentHost, agentPort) // FIXME: Api doesn't allow closing the socket
  import com.abdulradi.opentracing.xray.v1.Format._

  override def report(span: Span): Unit =
    SpanConverter(span).map(t => sender.send(t))
}
