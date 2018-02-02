/*
 * Copyright 2017 com.abdulradi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abdulradi.opentracing.xray.v1.model

import com.abdulradi.opentracing.xray.utils.refined.Hex
import io.circe.JsonObject

sealed trait TopLevelTrace

final case class Segment(
  topLevelFields: TopLevelFields,
  /**
    * An object with information about your application.
    */
  service: Option[Service],
  /**
    * A string that identifies the user who sent the request.
    */
  user: Option[String],
  /**
    * The type of AWS resource running your application.
    * When multiple values are applicable to your application, use the one that is most specific. For example, a Multicontainer Docker Elastic Beanstalk environment runs your application on an Amazon ECS container, which in turn runs on an Amazon EC2 instance. In this case you would set the origin to AWS::ElasticBeanstalk::Environment as the environment is the parent of the other two resources.
    */
  origin: Option[Origin],
  /**
    * A subsegment ID you specify if the request originated from an instrumented application. The X-Ray SDK adds the parent subsegment ID to the tracing header for downstream HTTP calls.
    */
  parentId: Option[Hex._16],

  /**
    * Information about the original HTTP request.
    */
  http: Option[ServedHttp],
  /**
    * aws object with information about the AWS resource on which your application served the request
    */
  aws: Option[SegmentAws]
) extends TopLevelTrace

/**
  * type=subsegment
  */
final case class IndependentSubsegment(
  topLevelFields: TopLevelFields,
  subsegmentFields: SubsegmentFields,
  /**
    * A subsegment ID you specify if the request originated from an instrumented application. The X-Ray SDK adds the parent subsegment ID to the tracing header for downstream HTTP calls.
    */
  parentId: SegmentId
) extends TopLevelTrace

final case class Subsegment(
  commonFields: CommonFields,
  subsegmentFields: SubsegmentFields
)

final case class CommonFields(
  /**
    * A 64-bit identifier for the segment, unique among segments in the same trace, in 16 hexadecimal digits.
    * or: A 64-bit identifier for the subsegment, unique among segments in the same trace, in 16 hexadecimal digits.
    */
  id: SegmentId,

  /**
    * The logical name of the service that handled the request, up to 200 characters. For example, your application's name or domain name. Names can contain Unicode letters, numbers, and whitespace, and the following symbols: _, ., :, /, %, &, #, =, +, \, -, @
    * OR: The logical name of the subsegment. For downstream calls, name the subsegment after the resource or service called. For custom subsegments, name the subsegment after the code that it instruments (e.g., a function name).
    *
    * A segment's name should match the domain name or logical name of the service that generates the segment. However, this is not enforced. Any application that has permission to PutTraceSegments can send segments with any name.
    */
  name: String,

  /**
    * number that is the time the segment was created, in floating point seconds in epoch time. For example, 1480615200.010 or 1.480615200010E9. Use as many decimal places as you need. Microsecond resolution is recommended when available.
    * or number that is the time the subsegment was created, in floating point seconds in epoch time, accurate to milliseconds. For example, 1480615200.010 or 1.480615200010E9.
    */
  startTime: Double,

  /**
    * number that is the time the segment was closed. For example, 1480615200.090 or 1.480615200090E9. Specify either an end_time or in_progress.
    * OR: number that is the time the subsegment was closed. For example, 1480615200.090 or 1.480615200090E9. Specify an end_time or in_progress.
    *
    * if missing then in_progress = true. instead of specifying an end_time to record that a subsegment is started, but is not complete. Only send one complete subsegment, and one or zero in-progress subsegments, per downstream request.
    */
  endTime: Option[Double],

  /**
   * error fields that indicate an error occurred and that include information about the exception that caused the error.
   */
  errorFields: Option[CommonErrorFields],

  /**
    * annotations object with key-value pairs that you want X-Ray to index for search.
    * Optional
    */
  annotations: Map[AnnotationKey, AnnotationValue],

  /**
    * metadata object with any additional data that you want to store in the segment.
    */
  metadata: Option[JsonObject],

  /**
    * array of subsegment objects.
    */
  subsegments: Seq[Subsegment]
)

final case class TopLevelFields(
  commonFields: CommonFields,
  /**
    * A unique identifier that connects all segments and subsegments originating from a single client request.
    */
  traceId: TraceId
)

final case class SubsegmentFields(
  /**
    * aws for AWS SDK calls; remote for other downstream calls.
    */
  namespace: Option[Namespace],
  /**
    * array of subsegment IDs that identifies subsegments with the same parent that completed prior to this subsegment.
    */
  precursorIds: Seq[SegmentId],
  /**
    * http object with information about an outgoing HTTP call.
    */
  http: Option[DownstreamHttp],
  /**
    * aws object with information about the downstream AWS resource that your application called.
    */
  aws: Option[SubsegmentAws],
  /**
    * queries that your application makes to an SQL database.
    */
  sql: Option[Sql]
)

/**
  * aws for AWS SDK calls; remote for other downstream calls.
  */
sealed trait Namespace

object Namespace {
  final case object Aws extends Namespace
  final case object Remote extends Namespace
}

sealed trait Origin
case object AWSEC2Instance extends Origin
case object AWSECSContainer extends Origin
case object AWSElasticBeanstalkEnvironment extends Origin

/**
  * An object with information about your application.
  */
final case class Service(
  /**
    * A string that identifies the version of your application that served the request.
    */
  version: String
)

/**
  * A unique identifier that connects all segments and subsegments originating from a single client request.
  *
  * Trace ID Format:
  * A trace_id consists of three numbers separated by hyphens. For example, 1-58406520-a006649127e371903a2de979. This includes:
  * The version number, that is, 1.
  * The time of the original request, in Unix epoch time, in 8 hexadecimal digits.
  * For example, 10:00AM December 2nd, 2016 PST in epoch time is 1480615200 seconds, or 58406520 in hexadecimal.
  * A 96-bit identifier for the trace, globally unique, in 24 hexadecimal digits.
  *
  * Trace ID Security:
  * Trace IDs are visible in response headers. Generate trace IDs with a secure random algorithm to ensure that attackers cannot calculate future trace IDs and send requests with those IDs to your application.
  */
final case class TraceId(
  /**
    * The time of the original request, in Unix epoch time, in 8 hexadecimal digits.
    */
  originalRequestTime: Hex._8,
  identifier: Hex._24
)
