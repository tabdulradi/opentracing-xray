package com.abdulradi.opentracing.xray.v1.zipkin

import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.refineV
import zipkin2.Span
import scala.collection.JavaConverters._
import io.circe.{Json, JsonObject}
import com.abdulradi.opentracing.xray.utils.RichOptionOfEither._
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11

private[zipkin] object SpanConverter extends (Span => Either[String, TopLevelTrace]) {

  private def parseTraceId(traceId: String): Either[String, TraceId] =
    if (traceId.length != 32) {
      Left(s"Span reported without a 128-bit trace ID. traceId=$traceId")
    } else {
      val (originalRequestTime, identifier) = traceId.splitAt(8)
      for {
        time <- refineV[Hex.P8](originalRequestTime)
        id <- refineV[Hex.P24](identifier)
      } yield TraceId(time, id)
    }

  private def fromNullable[T](t: T, msgIfEmpty: => String): Either[String, T] =
    Option(t).toRight(msgIfEmpty)

  /**
    * XRay: http://docs.aws.amazon.com/xray/latest/devguide/xray-concepts.html?shortFooter=true#xray-concepts-traces
    *   - A (Request) Trace consists of Segments (Highlevel, probably one per microsevice per request)
    *   - A Segment might consists of Subsegments (Can b granular to instrument specific functions or lines of code in your application or calling external services)
    *
    * Terminology Mapping
    * Zipkin/Opentracing <----> XRay
    * Trace                     Trace
    * Span                      Segment
    * ???                       Subsegment
    *
    * @param span
    * @return
    */
  def apply(span: Span): Either[String, TopLevelTrace] = {
    val endTime: Option[Double] = Option(span.duration()).map(_.longValue + span.timestamp.longValue).map(_ / 1000000.0D) // microseconds to seconds

    val (bigTags, smallTags) = span.tags().asScala.toMap.partition { case (key, value) => key.length > 250 || value.length > 250 }
    val (bigAnnotations, smallAnnotation) = span.annotations().asScala.map(a => a.timestamp().toString -> a.value()).partition(_._2.length > 250)

    val parentId = Option(span.parentId()).filterNot("0000000000000000".equals) // XRayExtractor sets this dummy value in case there is no parent

    val annotations: Map[AnnotationKey, AnnotationValue] =
      (smallTags ++ smallAnnotation).map {
        case (key, value) => AnnotationKey.escape(key) -> StringAnnotation(value)
      }

    val metadata: Option[JsonObject] =
      Some(JsonObject.fromMap(
        (bigTags ++ bigAnnotations).map {
          case (key, value) => key -> Json.fromString(value)
        }
      ))

    //    error: Boolean = None // TODO indicating that a client error occurred (response status code was 4XX Client Error)
    //    throttle: Boolean = None // TODO indicating that a request was throttled (response status code was 429 Too Many Requests).
    //    fault: Boolean = None // TODO indicating that a server error occurred (response status code was 5XX Server Error).
    //    cause: Cause = None // TODO Indicate the cause of the error
    val errorFields = None // TODO Some(CommonErrorFields(error, throttle, fault, cause))

    val http: Option[SegmentHttp] = None // TODO information about the original HTTP request.
    val aws: Option[SegmentAws] = None // TODO information about the AWS resource on which your application served the request

    val subsegments = Seq.empty[Subsegment] // TODO

    val service: Option[Service] = None // TODO information about your application.
    val user: Option[String] = None // TODO  user who sent the request.
    val origin: Option[Origin] = None // TODO type of AWS resource running your application.

    //    Unused fields:
    //    span.remoteEndpoint()
    //    span.kind()
    //    span.remoteServiceName()
    //    span.shared()

    // FIXME: We encountering weird behaviour of for asking for "withFilter is not a member of Either" although there is no if
    parseTraceId(span.traceId()).flatMap { traceId =>
      refineV[Hex.P16](span.id()).flatMap { id =>
        fromNullable(span.localServiceName, "span.localServiceName == null").flatMap { name =>
          fromNullable(span.timestamp, "span.timestamp == null").flatMap { startTimeMicro =>
            val startTime = startTimeMicro.longValue() / 1000000.0D // microseconds to seconds
            parentId.map(id => refineV[Hex.P16](id)).toEitherOfOption.map { parentId =>
              Segment(
                TopLevelFields(
                  CommonFields(id, name, startTime, endTime, http, aws, errorFields, annotations, metadata, subsegments),
                  traceId
                ), service, user, origin, parentId)
            }
          }
        }
      }
    }
  }
}
