package com.abdulradi.opentracing.xray.v1.format

import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.string.Url
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._


object http {
  implicit val segmentHttpEncoder: Encoder[SegmentHttp] = deriveEncoder[SegmentHttp]
  implicit val segmentHttpDecoder: Decoder[SegmentHttp] = deriveDecoder[SegmentHttp]

  implicit val subSegmentHttpEncoder: Encoder[SubsegmentHttp] = deriveEncoder[SubsegmentHttp]
  implicit val subSegmentHttpDecoder: Decoder[SubsegmentHttp] = deriveDecoder[SubsegmentHttp]

  implicit val segmentRequestEncoder: Encoder[SegmentRequest] = deriveEncoder[SegmentRequest]
  implicit val segmentRequestDecoder: Decoder[SegmentRequest] = deriveDecoder[SegmentRequest]

  implicit val subSegmentRequestEncoder: Encoder[SubsegmentRequest] = deriveEncoder[SubsegmentRequest]
  implicit val subSegmentRequestDecoder: Decoder[SubsegmentRequest] = deriveDecoder[SubsegmentRequest]

  implicit val commonRequestFieldsEncoder: Encoder[CommonRequestFields] = deriveEncoder[CommonRequestFields]
  implicit val commonRequestFieldsDecoder: Decoder[CommonRequestFields] = deriveDecoder[CommonRequestFields]

  implicit val responseEncoder: Encoder[Response] = deriveEncoder[Response]
  implicit val responseDecoder: Decoder[Response] = deriveDecoder[Response]

  implicit val urlEncoder: Encoder[Url] = deriveEncoder[Url]
  implicit val urlDecoder: Decoder[Url] = deriveDecoder[Url]
}
