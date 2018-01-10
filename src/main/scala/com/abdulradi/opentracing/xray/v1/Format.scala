package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.v1.jaeger.ConversionOps
import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.api.Refined
import io.circe._
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
  implicit val segmentHttpEncoder: ObjectEncoder[SegmentHttp] = ObjectEncoder.instance(obj => JsonObject(
    "request" -> obj.request.asJson,
    "response" -> obj.response.asJson
  ))
  implicit val subSegmentHttpEncoder: ObjectEncoder[SubsegmentHttp] = ObjectEncoder.instance(obj => JsonObject(
    "request" -> obj.request.asJson,
    "response" -> obj.response.asJson
  ))
  implicit val subSegmentRequestEncoder: ObjectEncoder[SubsegmentRequest] = ObjectEncoder.instance[SubsegmentRequest] { subsegmentRequest =>
    ("traced" -> subsegmentRequest.traced.asJson) +: subsegmentRequest.commonFields.asJsonObject
  }


  /* ************************
   * AWS
   **************************/
  implicit val segmentAwsEncoder: ObjectEncoder[SegmentAws] = ObjectEncoder.instance[SegmentAws](segmentAws =>
    JsonObject(
      "account_id" -> segmentAws.accountId.asJson,
      "ecs" -> segmentAws.ecs.asJson,
      "ec2" -> segmentAws.ec2.asJson,
      "elastic_beanstalk" -> segmentAws.elasticBeanstalk.asJson
    )
  )

  implicit val ecsEncoder: ObjectEncoder[Ecs] = ObjectEncoder.instance[Ecs](obj =>
    JsonObject(
      "container" -> obj.container.asJson
    )
  )
  implicit val ec2Encoder: ObjectEncoder[Ec2] = Encoder.forProduct2("instance_id", "availability_zone")(ec2 => (ec2.instanceId, ec2.availabilityZone))
  implicit val elasticBeanstalkEncoder: ObjectEncoder[ElasticBeanstalk] = Encoder.forProduct3("environment_name", "version_label", "deployment_id")(elb => (elb.environmentName, elb.versionLabel, elb.deploymentId))
  implicit val subSegmentAwsEncoder: ObjectEncoder[SubsegmentAws] = Encoder.forProduct6("operation", "account_id", "region", "request_id", "queue_url", "table_name")(sa => (sa.operation, sa.accountId, sa.region, sa.requestId, sa.queueUrl, sa.tableName))

  /* ************************
   * Errors
   **************************/
  implicit val commonErrorFieldsEncoder: ObjectEncoder[CommonErrorFields] = ObjectEncoder.instance[CommonErrorFields](obj =>
    JsonObject(
      "error" -> obj.error.asJson,
      "throttle" -> obj.throttle.asJson,
      "fault" -> obj.fault.asJson,
      "cause" -> obj.cause.asJson
    )
  )
  implicit val causeEncoder: ObjectEncoder[Cause] = ObjectEncoder.instance[Cause] {
      case id: ExceptionId =>
        JsonObject(
        "value" -> id.value.asJson
        )
      case obj: CauseObject =>
        JsonObject(
        "working_directory" -> obj.workingDirectory.asJson,
        "paths" -> obj.paths.asJson,
        "exceptions" -> obj.exceptions.asJson
      )
  }

  implicit val exceptionDetailsEncoder: ObjectEncoder[ExceptionDetails] = ObjectEncoder.instance[ExceptionDetails](obj =>
    JsonObject(
      "id" -> obj.id.asJson,
      "message" -> obj.message.asJson,
      "type" -> obj.`type`.asJson,
      "remote" -> obj.remote.asJson,
      "truncated" -> obj.truncated.asJson,
      "skipped" -> obj.skipped.asJson,
      "cause" -> obj.cause.asJson,
      "stack" -> obj.stack.asJson
    )
  )

  implicit val stackFrameEncoder: ObjectEncoder[StackFrame] = ObjectEncoder.instance[StackFrame](obj =>
    JsonObject(
      "path" -> obj.path.asJson,
      "line" -> obj.line.asJson,
      "label" -> obj.label.asJson
    )
  )

  implicit val annotationValueEncoder: Encoder[AnnotationValue] = new Encoder[AnnotationValue]() {
    def apply(value: AnnotationValue) = value match {
      case StringAnnotation(value) => value.asJson
      case IntAnnotation(value) => value.asJson
      case DoubleAnnotation(value) => value.asJson
      case BooleanAnnotation(value) => value.asJson
    }
  }

  implicit val sqlEncoder: ObjectEncoder[Sql] = ObjectEncoder.instance[Sql](obj =>
    JsonObject(
      "connection_string" -> obj.connectionString.asJson,
      "url" -> obj.url.asJson,
      "sanitized_query" -> obj.sanitized_query.asJson,
      "database_type" -> obj.database_type.asJson,
      "database_version" -> obj.database_version.asJson,
      "driver_version" -> obj.driver_version.asJson,
      "user" -> obj.user.asJson,
      "preparation" -> obj.preparation.asJson
      )
  )
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
  implicit val serviceEncoder: ObjectEncoder[Service] = ObjectEncoder.instance[Service](obj =>
    JsonObject(
      "version" -> obj.version.asJson
    )
  )
  implicit val subSegmentFieldsEncoder: ObjectEncoder[SubsegmentFields] = ObjectEncoder.instance[SubsegmentFields](obj =>
    merge()(
      "namespace" -> obj.namespace.asJson ,
      "precursor_ids" -> obj.precursorIds.asJson ,
      "http" -> obj.http.asJson,
      "aws" -> obj.aws.asJson,
      "sql" -> obj.sql.asJson
    )
  )

  implicit def mapWithStringRefinedKey[KP, V](implicit underlying: ObjectEncoder[Map[String, V]]): ObjectEncoder[Map[String Refined KP, V]] =
    underlying.contramapObject[Map[String Refined KP, V]](_.map { case (k, v) => k.value -> v }.toMap)

  implicit val subSegmentEncoder: ObjectEncoder[Subsegment] = ObjectEncoder.instance[Subsegment](obj =>
    merge(obj.commonFields.asJsonObject, obj.subsegmentFields.asJsonObject)()
  )

  implicit val commonFieldsEncoder: ObjectEncoder[CommonFields] = ObjectEncoder.instance[CommonFields](obj =>
    ("id" -> obj.id.asJson) +:
      ("name" -> obj.name.asJson) +:
      ("start_time" -> obj.startTime.asJson) +:
      ("end_time" -> obj.endTime.asJson) +:
      ("http" -> obj.http.asJson) +:
      ("aws" -> obj.aws.asJson) +:
      ("annotations" -> obj.annotations.asJson) +:
      ("metadata" -> obj.metadata.asJson) +:
      ("subsegments" -> obj.subsegments.asJson) +: obj.errorFields.map(_.asJsonObject).getOrElse(JsonObject.empty)
  )

  implicit val traceIdEncoder: Encoder[TraceId] = {
    import ConversionOps.TraceIdConversionOps
    Encoder.instance[TraceId](tid => Json.fromString(tid.toHeaderString))
  }

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




