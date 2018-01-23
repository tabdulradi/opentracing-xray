package com.abdulradi.opentracing.xray.v1.model

/**
  * Structures that holds whatever data we receive in the tracing header
  *
  * Example Tracing header with root trace ID and sampling decision
  * X-Amzn-Trace-Id: Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1
  *
  * Example Tracing header with root trace ID, parent segment ID and sampling decision
  * X-Amzn-Trace-Id: Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1
  */
final case class TracingHeader(
  rootTraceId: TraceId,
  parentSegmentId: Option[SegmentId],
  samplingDecision: Option[Int]
)

object TracingHeader {
  object Keys {
    val HttpHeaderKey = "X-Amzn-Trace-Id"
    val Root = "Root"
    val Parent = "Parent"
    val Sampled = "Sampled"
  }
}