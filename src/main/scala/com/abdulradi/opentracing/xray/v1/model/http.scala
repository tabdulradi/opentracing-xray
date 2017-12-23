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

import eu.timepit.refined.string.Url

final case class SegmentHttp(
  request: Option[SegmentRequest],
  response: Option[Response]
)

final case class SubsegmentHttp(
  request: Option[SubsegmentRequest],
  response: Option[Response]
)

final case class SegmentRequest(
  commonFields: CommonRequestFields,
  /**
    * boolean indicating that the client_ip was read from an X-Forwarded-For header and is not reliable as it could have been forged.
    */
  xForwardedFor: Option[Boolean]
)
final case class SubsegmentRequest(
  commonFields: CommonRequestFields,
  /**
    * (subsegments only) boolean indicating that the downstream call is to another traced service. If this field is set to true, X-Ray considers the trace to be broken until the downstream service uploads a segment with a parent_id that matches the id of the subsegment that contains this block.
    */
  traced: Option[Boolean]
)

final case class CommonRequestFields(
  /**
    * The request method. For example, GET.
    */
  method: Option[String],
  /**
    * The full URL of the request, compiled from the protocol, hostname, and path of the request.
    */
  url: Option[Url],
  /**
    * The user agent string from the requester's client.
    */
  userAgent: Option[String],
  /**
    * The IP address of the requester. Can be retrieved from the IP packet's Source Address or, for forwarded requests, from an X-Forwarded-For header.
    * TODO: Refined but don't forget IPv6
    */
  clientIp: Option[String]
)

final case class Response(
  /**
    * number indicating the HTTP status of the response.
    */
  status: Option[Int],
  /**
    * number indicating the length of the response body in bytes.
    */
  contentLength: Option[Int]
)
