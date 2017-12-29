package com.abdulradi.opentracing.xray.v1.format

import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.refined.refinedEncoder


object http {

  implicit val segmentHttpEncoder: Encoder[SegmentHttp] = deriveEncoder[SegmentHttp]

  implicit val subSegmentHttpEncoder: Encoder[SubsegmentHttp] = deriveEncoder[SubsegmentHttp]

  implicit val segmentRequestEncoder: Encoder[SegmentRequest] = deriveEncoder[SegmentRequest]

  implicit val subSegmentRequestEncoder: Encoder[SubsegmentRequest] = deriveEncoder[SubsegmentRequest]

  implicit val responseEncoder: Encoder[Response] =
    Encoder.forProduct2("status", "content_length")(res => (res.status, res.contentLength))

  implicit val urlEncoder: Encoder[String Refined Url] = refinedEncoder

  implicit val commonRequestFieldsEncoder: Encoder[CommonRequestFields] =
    Encoder.forProduct4("method", "url", "user_agent", "client_ip")(crf =>
      (crf.method, crf.url, crf.userAgent, crf.clientIp))
}

