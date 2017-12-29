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


import com.abdulradi.opentracing.xray.utils.refined.Alphanumeric.Path
import com.abdulradi.opentracing.xray.utils.refined.Hex

/**
  * Error fields that indicate an error occurred and that include information about the exception that caused the error.
  *
  * Set one or more of the following fields to true to indicate that an error occurred. Multiple types can apply if errors compound. For example, a 429 Too Many Requests error from a downstream call may cause your application to return 500 Internal Server Error, in which case all three types would apply.
  */
final case class CommonErrorFields(
  /**
    * indicating that a client error occurred (response status code was 4XX Client Error)
    */
  error: Boolean,
  /**
    * indicating that a request was throttled (response status code was 429 Too Many Requests).
    */
  throttle: Boolean,
  /**
    * indicating that a server error occurred (response status code was 5XX Server Error).
    */
  fault: Boolean,
  /**
    * Indicate the cause of the error
    */
  cause: Cause
)

sealed trait Cause
final case class ExceptionId(value: Hex._16) extends Cause
final case class CauseObject(
  /**
    * The full path of the working directory when the exception occurred.
    */
  workingDirectory: Path,
  /**
    * The array of paths to libraries or modules in use when the exception occurred.
    */
  paths: Seq[Path],
  /**
    * The array of exception objects.
    */
  exceptions: Seq[ExceptionDetails]
)

case class ExceptionDetails(
  /**
    * A 64-bit identifier for the exception, unique among segments in the same trace, in 16 hexadecimal digits.
    */
  id: Hex._16,
  /**
    * The exception message.
    */
  message: Option[String],
  /**
    * The exception type.
    */
  `type`: Option[String],
  /**
    * boolean indicating that the exception was caused by an error returned by a downstream service.
    */
  remote: Option[Boolean],
  /**
    * integer indicating the number of stack frames that are omitted from the stack.
    */
  truncated: Option[Int],
  /**
    * integer indicating the number of exceptions that were skipped between this exception and its child, that is, the exception that it caused.
    */
  skipped: Option[Int],
  /**
    * Exception ID of the exception's parent, that is, the exception that caused this exception.
    */
  cause: Option[Hex._16],
  /**
    * array of stackFrame objects.
    * Optional
    */
  stack: Seq[StackFrame]
)

final case class StackFrame(
  /**
    * The relative path to the file.
    */
  path: Option[String],
  /**
    * The line in the file.
    */
  line: Option[Int],
  /**
    * The function or method name.
    */
  label: Option[String]
)
