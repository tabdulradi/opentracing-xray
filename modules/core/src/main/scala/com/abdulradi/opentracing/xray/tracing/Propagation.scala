package com.abdulradi.opentracing.xray.tracing

import cats.syntax.either._ // Needed to cross-compile to Scala 2.11
import com.abdulradi.opentracing.xray.v1.model.{TraceId, TracingHeader}
import com.abdulradi.opentracing.xray.utils.RichOptionOfEither._

private[xray] object Propagation {
  import ConversionOps._
  val HeaderKeyName = "X-Amzn-Trace-Id" // X-Amzn-Trace-Id: Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1

  def parseOrGenerate(headers: String => Option[String]): TracingHeader =
    parseHeaders(headers) match {
      case Some(Right(header)) => header
      case _ => generateHeader()
    }

  def generateHeader(): TracingHeader =
    TracingHeader(TraceId.generate(), None, None, Map.empty)

  def parseHeaders(headers: String => Option[String]): Option[Either[String, TracingHeader]] =
    headers(Propagation.HeaderKeyName).map(parseHeaderValue)

  def parseHeaderValue(headerValue: String): Either[String, TracingHeader] =
    for {
      data <- Parsers.parseTracingHeader(headerValue)
      rootTraceIdStr <- data.get(TracingHeader.Keys.Root).toRight("Root key is required")
      rootTraceId <- Parsers.parseTraceId(rootTraceIdStr)
      parentSegmentID <- data.get(TracingHeader.Keys.Parent).map(Parsers.parseSegmentId).toEitherOfOption
    } yield
      TracingHeader(
        rootTraceId,
        parentSegmentID,
        data.get(TracingHeader.Keys.Sampled).collect {
          case "0" => false
          case "1" => true
        },
        data.filterKeys(k => ! Set(
          TracingHeader.Keys.Root,
          TracingHeader.Keys.Parent,
          TracingHeader.Keys.Sampled
        ).contains(k))
      )
}

