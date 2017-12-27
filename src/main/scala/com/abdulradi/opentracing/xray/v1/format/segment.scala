package com.abdulradi.opentracing.xray.v1.format


import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model._
import io.circe._
import io.circe.JsonObject
import io.circe.generic.semiauto._
import io.circe.refined.refinedEncoder


object segment {

  implicit val topLevelTraceEncoder: Encoder[TopLevelTrace] = deriveEncoder[TopLevelTrace]
  implicit val topLevelTraceDecoder: Decoder[TopLevelTrace] = deriveDecoder[TopLevelTrace]

  implicit val subSegmentEncoder: Encoder[Subsegment] = deriveEncoder[Subsegment]
  implicit val subSegmentDecoder: Decoder[Subsegment] = deriveDecoder[Subsegment]

  implicit val commonFieldsEncoder: Encoder[CommonFields] = deriveEncoder[CommonFields]
  implicit val commonFieldsDecoder: Decoder[CommonFields] = deriveDecoder[CommonFields]

  implicit val topLevelFieldsEncoder: Encoder[TopLevelFields] = deriveEncoder[TopLevelFields]
  implicit val topLevelFieldsDecoder: Decoder[TopLevelFields] = deriveDecoder[TopLevelFields]

  implicit val subSegmentFieldsEncoder: Encoder[SubsegmentFields] = deriveEncoder[SubsegmentFields]
  implicit val subSegmentFieldsDecoder: Decoder[SubsegmentFields] = deriveDecoder[SubsegmentFields]

  implicit val namespaceEncoder: Encoder[Namespace] = deriveEncoder[Namespace]
  implicit val namespaceDecoder: Decoder[Namespace] = deriveDecoder[Namespace]

  implicit val originEncoder: Encoder[Origin] = deriveEncoder[Origin]
  implicit val originDecoder: Decoder[Origin] = deriveDecoder[Origin]

  implicit val jsonObjectEncoder: Encoder[JsonObject] = deriveEncoder[JsonObject]
  implicit val jsonObjectDecoder: Decoder[JsonObject] = deriveDecoder[JsonObject]

  implicit val serviceEncoder: Encoder[Service] = deriveEncoder[Service]
  implicit val serviceDecoder: Decoder[Service] = deriveDecoder[Service]

  implicit val traceIdEncoder: Encoder[TraceId] = deriveEncoder[TraceId]
  implicit val traceIdDecoder: Decoder[TraceId] = deriveDecoder[TraceId]

  implicit val annotationKeyEncoder: Encoder[AnnotationKey] = refinedEncoder
  implicit val segmentIdEncoder: Encoder[SegmentId] = refinedEncoder

  implicit val hex8Encoder: Encoder[Hex._8] = refinedEncoder
  implicit val hex24Encoder: Encoder[Hex._24] = refinedEncoder
}





