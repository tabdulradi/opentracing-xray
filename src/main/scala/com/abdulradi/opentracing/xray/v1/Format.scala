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
  implicit val segmentRequestEncoder: ObjectEncoder[SegmentRequest] = ObjectEncoder.instance[SegmentRequest] { segmentRequest =>
    ("x_forwarded_for" -> segmentRequest.xForwardedFor.asJson) +: segmentRequest.commonFields.asJsonObject
  }
  implicit val responseEncoder: ObjectEncoder[Response] = Encoder.forProduct2("status", "content_length")(res => (res.status, res.contentLength))
  implicit val segmentHttpEncoder: ObjectEncoder[SegmentHttp] = deriveEncoder[SegmentHttp]
  implicit val subSegmentHttpEncoder: ObjectEncoder[SubsegmentHttp] = deriveEncoder[SubsegmentHttp]
  implicit val subSegmentRequestEncoder: ObjectEncoder[SubsegmentRequest] = ObjectEncoder.instance[SubsegmentRequest] { subsegmentRequest =>
    ("traced" -> subsegmentRequest.traced.asJson) +: subsegmentRequest.commonFields.asJsonObject
  }


  /* ************************
   * AWS
   **************************/
  implicit val segmentAwsEncoder: ObjectEncoder[SegmentAws] = ObjectEncoder.instance[SegmentAws](segmentAws =>
    ("account_id" -> segmentAws.accountId.asJson) +:
      ("ecs" -> segmentAws.ecs.asJson) +:
      ("ec2" -> segmentAws.ec2.asJson) +:
      ("elastic_beanstalk" -> segmentAws.elasticBeanstalk.asJson)
  )
  //Encoder.forProduct4("account_id", "ecs", "ec2", "elastic_beanstalk")(sa => (sa.accountId, sa.ecs, sa.ec2, sa.elasticBeanstalk))
  implicit val ecsEncoder: ObjectEncoder[Ecs] = deriveEncoder[Ecs]
  implicit val ec2Encoder: ObjectEncoder[Ec2] = Encoder.forProduct2("instance_id", "availability_zone")(ec2 => (ec2.instanceId, ec2.availabilityZone))
  implicit val elasticBeanstalkEncoder: ObjectEncoder[ElasticBeanstalk] = Encoder.forProduct3("environment_name", "version_label", "deployment_id")(elb => (elb.environmentName, elb.versionLabel, elb.deploymentId))
  implicit val subSegmentAwsEncoder: ObjectEncoder[SubsegmentAws] = Encoder.forProduct6("operation", "account_id", "region", "request_id", "queue_url", "table_name")(sa => (sa.operation, sa.accountId, sa.region, sa.requestId, sa.queueUrl, sa.tableName))

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
  implicit val preparationEncoder: Encoder[Preparation] = Encoder.instance[Preparation] {
    pr => pr.toString.toLowerCase().asJson
  }

  /* ************************
   * Segment
   **************************/
  implicit val namespaceEncoder: Encoder[Namespace] = Encoder.instance[Namespace] {
    ns => ns.toString.toLowerCase().asJson
  }
  implicit val originEncoder: Encoder[Origin] = Encoder.instance[Origin] {
    origin => origin.toString.toLowerCase().asJson
  }
  implicit val serviceEncoder: ObjectEncoder[Service] = deriveEncoder[Service]
  implicit val subSegmentFieldsEncoder: ObjectEncoder[SubsegmentFields] = deriveEncoder[SubsegmentFields]

  implicit def mapWithStringRefinedKey[KP, V](implicit underlying: ObjectEncoder[Map[String, V]]): ObjectEncoder[Map[String Refined KP, V]] =
    underlying.contramapObject[Map[String Refined KP, V]](_.map { case (k, v) => k.value -> v }.toMap)

  implicit val subSegmentEncoder: ObjectEncoder[Subsegment] = deriveEncoder
  implicit val commonFieldsEncoder: ObjectEncoder[CommonFields] = deriveEncoder[CommonFields]
  implicit val traceIdEncoder: ObjectEncoder[TraceId] = deriveEncoder[TraceId]
  implicit val topLevelFieldsEncoder: ObjectEncoder[TopLevelFields] = ObjectEncoder.instance[TopLevelFields] { topLevelFields =>
    ("trace_id" -> topLevelFields.traceId.asJson) +: topLevelFields.commonFields.asJsonObject
  }

  implicit val independentSubsegmentEncoder: ObjectEncoder[IndependentSubsegment] = ObjectEncoder.instance[IndependentSubsegment](independentSubsegment =>
    merge(independentSubsegment.topLevelFields.asJsonObject, independentSubsegment.subsegmentFields.asJsonObject)(
      "parent_id" -> independentSubsegment.parentId.asJson
    )
  )

  implicit val SegmentEncoder: ObjectEncoder[Segment] = ObjectEncoder.instance[Segment](segment =>
    ("service" -> segment.service.asJson) +:
      ("user" -> segment.user.asJson) +:
      ("origin" -> segment.origin.asJson) elasticBeanstalk +:
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




