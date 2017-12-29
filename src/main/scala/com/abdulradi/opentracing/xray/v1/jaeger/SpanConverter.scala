package com.abdulradi.opentracing.xray.v1.jaeger

import com.abdulradi.opentracing.xray.v1.model._
import com.uber.jaeger.Span
import io.circe.{Json, JsonObject}
import scala.collection.JavaConverters._

object SpanConverter {
  import ConversionOps._

  /**
    * XRay: http://docs.aws.amazon.com/xray/latest/devguide/xray-concepts.html?shortFooter=true#xray-concepts-traces
    *   - A (Request) Trace consists of Segments (Highlevel, probably one per microsevice per request)
    *   - A Segment might consists of Subsegments (Can b granular to instrument specific functions or lines of code in your application or calling external services)
    *
    *
    * Jaeger: http://jaeger.readthedocs.io/en/latest/architecture/
    *   -
    *
    *
    * Terminology Mapping
    * Jaeger/Opentracing <----> XRay
    * Trace                     Trace
    * Span                      Segment
    * ???                       Subsegment
    *
    * @param span
    * @return
    */
  def convert(span: Span): Either[String, TopLevelTrace] = for {
    traceId <- TraceId.fromBaggage(span.context.baggageItems)

    id: SegmentId = SegmentId.fromOpenTracing(span.context.getSpanId)
    name: String = span.getServiceName
    startTime: Double = span.getStart.toDouble / 1000000 // microseconds to seconds
    endTime: Option[Double] = Option(span.getDuration).filterNot(_ == 0).map(_ + span.getStart).map(_.toDouble / 1000000)

//    error: Boolean = None // TODO indicating that a client error occurred (response status code was 4XX Client Error)
//    throttle: Boolean = None // TODO indicating that a request was throttled (response status code was 429 Too Many Requests).
//    fault: Boolean = None // TODO indicating that a server error occurred (response status code was 5XX Server Error).
//    cause: Cause = None // TODO Indicate the cause of the error
    errorFields = None // TODO Some(CommonErrorFields(error, throttle, fault, cause))

    http: Option[SegmentHttp] = None // TODO information about the original HTTP request.
    aws: Option[SegmentAws] = None // TODO information about the AWS resource on which your application served the request

    annotations: Map[AnnotationKey, AnnotationValue] =
      span.getTags.asScala.map(p => AnnotationKey.escape(p._1) -> p._2).map {
        case (key, value: String) => key -> StringAnnotation(value)
        case (key, value: Integer) => key -> IntAnnotation(value)
        case (key, value: java.lang.Double) => key -> DoubleAnnotation(value)
        case (key, value: java.lang.Boolean) => key -> BooleanAnnotation(value)
        case (key, value) => key -> StringAnnotation(value.toString)
      }.toMap

    metadata: Option[JsonObject] =
      Some(JsonObject.fromMap(span.context.baggageItems.asScala.map(entry => entry.getKey -> Json.fromString(entry.getValue)).toMap)) // TODO filter own custom fields we added like TraceId

    subsegments: Seq[Subsegment] = Seq.empty // TODO

    service: Option[Service] = None // TODO information about your application.
    user: Option[String] = None // TODO  user who sent the request.
    origin: Option[Origin] = None // TODO type of AWS resource running your application.
    parentId = SegmentId.fromOptional(span.context.getParentId)

//    Unused fields:
//    span.getLogs
//    span.getOperationName
//    span.getReferences
  } yield Segment(
    TopLevelFields(
      CommonFields(id, name, startTime, endTime, http, aws, errorFields, annotations, metadata, subsegments),
      traceId
    ), service, user, origin, parentId)
}
