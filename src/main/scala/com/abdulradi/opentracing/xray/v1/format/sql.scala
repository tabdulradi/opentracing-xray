package com.abdulradi.opentracing.xray.v1.format

import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.Url
import io.circe.{Decoder, Encoder}
import io.circe.refined.refinedEncoder
import io.circe.generic.semiauto._


object sql {
  implicit val sqlEncoder: Encoder[Sql] = deriveEncoder[Sql] //fails
  implicit val sqlDecoder: Decoder[Sql] = deriveDecoder[Sql]

  implicit val stringRefinedUrlEncoder: Encoder[String Refined Url] = refinedEncoder

  implicit val preparationEncoder: Encoder[Preparation] = deriveEncoder[Preparation]
  implicit val preparationDecoder: Decoder[Preparation] = deriveDecoder[Preparation]
}


