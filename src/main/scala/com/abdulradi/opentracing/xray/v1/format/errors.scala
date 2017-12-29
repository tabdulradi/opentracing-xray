package com.abdulradi.opentracing.xray.v1.format


import com.abdulradi.opentracing.xray.utils.refined.Alphanumeric.Path
import com.abdulradi.opentracing.xray.v1.model._
import com.abdulradi.opentracing.xray.utils.refined._
import io.circe._
import io.circe.refined._
import io.circe.generic.semiauto._


object errors {

  implicit val commonErrorFieldsEncoder: Encoder[CommonErrorFields] = deriveEncoder[CommonErrorFields]

  implicit val causeEncoder: Encoder[Cause] = deriveEncoder[Cause]

  implicit val causeObjectEncoder: Encoder[CauseObject] = deriveEncoder[CauseObject]

  implicit val exceptionDetailsEncoder: Encoder[ExceptionDetails] = deriveEncoder[ExceptionDetails]

  implicit val stackFrameEncoder: Encoder[StackFrame] = deriveEncoder[StackFrame]

  implicit val hex16Encoder: Encoder[Hex._16] = refinedEncoder
  implicit val pathEncoder: Encoder[Path] = refinedEncoder
}
