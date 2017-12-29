package com.abdulradi.opentracing.xray.v1.format


import com.abdulradi.opentracing.xray.v1.model._
import io.circe.Encoder
import io.circe.generic.semiauto._


object aws {
  implicit val segmentAwsEncoder: Encoder[SegmentAws] = deriveEncoder[SegmentAws]

  implicit val ecsEncoder: Encoder[Ecs] = deriveEncoder[Ecs]

  implicit val ec2Encoder: Encoder[Ec2] = deriveEncoder[Ec2]

  implicit val elasticBeanstalkEncoder: Encoder[ElasticBeanstalk] = deriveEncoder[ElasticBeanstalk]

  implicit val subSegmentAwsEncoder: Encoder[SubsegmentAws] = deriveEncoder[SubsegmentAws]
}

