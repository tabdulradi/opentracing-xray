package com.abdulradi.opentracing.xray.v1.format


import com.abdulradi.opentracing.xray.v1.model._
import io.circe.Encoder
import io.circe.generic.semiauto._

object annotations {
  implicit val annotationValueEncoder: Encoder[AnnotationValue] = deriveEncoder[AnnotationValue]
}
