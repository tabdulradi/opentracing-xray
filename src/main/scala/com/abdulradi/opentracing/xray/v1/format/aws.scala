package com.abdulradi.opentracing.xray.v1.format


import com.abdulradi.opentracing.xray.v1.model._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._


object aws {
  implicit val segmentAwsEncoder: Encoder[SegmentAws] = deriveEncoder[SegmentAws]
  implicit val segmentAwsDecoder: Decoder[SegmentAws] = deriveDecoder[SegmentAws]

  implicit val ecsEncoder: Encoder[Ecs] = deriveEncoder[Ecs]
  implicit val ecsDecoder: Decoder[Ecs] = deriveDecoder[Ecs]

  implicit val ec2Encoder: Encoder[Ec2] = deriveEncoder[Ec2]
  implicit val ec2Decoder: Decoder[Ec2] = deriveDecoder[Ec2]

  implicit val elasticBeanstalkEncoder: Encoder[ElasticBeanstalk] = deriveEncoder[ElasticBeanstalk]
  implicit val elasticBeanstalkDecoder: Decoder[ElasticBeanstalk] = deriveDecoder[ElasticBeanstalk]

  implicit val subSegmentAwsEncoder: Encoder[SubsegmentAws] = deriveEncoder[SubsegmentAws]
  implicit val subSegmentAwsDecoder: Decoder[SubsegmentAws] = deriveDecoder[SubsegmentAws]
}

