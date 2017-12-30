package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.api.Refined
import io.circe._
import io.circe.generic.semiauto._
import io.circe.refined.refinedEncoder
import io.circe.syntax._

object Format {
  /* ************************
   * HTTP
   **************************/
  implicit val commonRequestFieldsEncoder: ObjectEncoder[CommonRequestFields] = Encoder.forProduct4("method", "url", "user_agent", "client_ip")(crf => (crf.method, crf.url, crf.userAgent, crf.clientIp))
  implicit val segmentRequestEncoder: ObjectEncoder[SegmentRequest] = deriveEncoder[SegmentRequest]
  implicit val responseEncoder: ObjectEncoder[Response] = Encoder.forProduct2("status", "content_length")(res => (res.status, res.contentLength))
  implicit val segmentHttpEncoder: ObjectEncoder[SegmentHttp] = deriveEncoder[SegmentHttp]
  implicit val subSegmentHttpEncoder: ObjectEncoder[SubsegmentHttp] = deriveEncoder[SubsegmentHttp]
  implicit val subSegmentRequestEncoder: ObjectEncoder[SubsegmentRequest] = deriveEncoder[SubsegmentRequest]


  /* ************************
   * AWS
   **************************/
  implicit val segmentAwsEncoder: ObjectEncoder[SegmentAws] = deriveEncoder[SegmentAws]
  implicit val ecsEncoder: ObjectEncoder[Ecs] = deriveEncoder[Ecs]
  implicit val ec2Encoder: ObjectEncoder[Ec2] = deriveEncoder[Ec2]
  implicit val elasticBeanstalkEncoder: ObjectEncoder[ElasticBeanstalk] = deriveEncoder[ElasticBeanstalk]
  implicit val subSegmentAwsEncoder: ObjectEncoder[SubsegmentAws] = deriveEncoder[SubsegmentAws]

  /* ************************
   * Errors
   **************************/
  implicit val commonErrorFieldsEncoder: ObjectEncoder[CommonErrorFields] = deriveEncoder[CommonErrorFields]
  implicit val causeEncoder: ObjectEncoder[Cause] = deriveEncoder[Cause]
  implicit val causeObjectEncoder: ObjectEncoder[CauseObject] = deriveEncoder[CauseObject]
  implicit val exceptionDetailsEncoder: ObjectEncoder[ExceptionDetails] = deriveEncoder[ExceptionDetails]
  implicit val stackFrameEncoder: ObjectEncoder[StackFrame] = deriveEncoder[StackFrame]


  implicit val annotationValueEncoder: ObjectEncoder[AnnotationValue] = deriveEncoder[AnnotationValue]

  implicit val sqlEncoder: ObjectEncoder[Sql] = deriveEncoder[Sql]
  implicit val preparationEncoder: ObjectEncoder[Preparation] = deriveEncoder[Preparation]


  implicit val namespaceEncoder: ObjectEncoder[Namespace] = deriveEncoder[Namespace]
  implicit val originEncoder: ObjectEncoder[Origin] = deriveEncoder[Origin]
  implicit val serviceEncoder: ObjectEncoder[Service] = deriveEncoder[Service]

  implicit val subSegmentFieldsEncoder: ObjectEncoder[SubsegmentFields] = deriveEncoder[SubsegmentFields]

  implicit def mapWithStringRefinedKey[KP, V](implicit underlying: ObjectEncoder[Map[String, V]]): ObjectEncoder[Map[String Refined KP, V]] =
    underlying.contramapObject[Map[String Refined KP, V]](_.map { case (k, v) => k.value -> v}.toMap)

  implicit val subSegmentEncoder: ObjectEncoder[Subsegment] = deriveEncoder
  implicit val commonFieldsEncoder: ObjectEncoder[CommonFields] = deriveEncoder[CommonFields]
  implicit val traceIdEncoder: ObjectEncoder[TraceId] = deriveEncoder[TraceId]
  implicit val topLevelFieldsEncoder: ObjectEncoder[TopLevelFields] = ObjectEncoder.instance[TopLevelFields] { topLevelFields =>
    ("trace_id" -> topLevelFields.traceId.asJson) +: topLevelFields.commonFields.asJsonObject
  }

  implicit val independentSubsegmentEncoder: ObjectEncoder[IndependentSubsegment] = ObjectEncoder.instance[IndependentSubsegment]( independentSubsegment =>
    merge(independentSubsegment.topLevelFields.asJsonObject, independentSubsegment.subsegmentFields.asJsonObject)(
      "parent_id" -> independentSubsegment.parentId.asJson
    )
  )

  implicit val SegmentEncoder: ObjectEncoder[Segment] = ObjectEncoder.instance[Segment](segment =>
    ("service" -> segment.service.asJson) +:
      ("user" -> segment.user.asJson) +:
      ("origin" -> segment.origin.asJson) +:
      ("parent_id" -> segment.parentId.asJson) +:
      segment.topLevelFields.asJsonObject
  )

  implicit val topLevelTraceEncoder: ObjectEncoder[TopLevelTrace] = ObjectEncoder.instance[TopLevelTrace] {
    case segment: Segment => segment.asJsonObject
    case independentSubsegment: IndependentSubsegment => independentSubsegment.asJsonObject
  }

  private def merge(objects: JsonObject*)(keyValues: (String, Json)*) =
    objects.foldLeft(keyValues.toMap) {
      case (acc, obj) => acc ++ obj.toMap
    }.asJsonObject
}




