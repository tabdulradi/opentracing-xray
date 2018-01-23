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

package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.utils.refined.{Alphanumeric, Hex}
import eu.timepit.refined.refineV

package object model {

  /**
    * Keys must be alphanumeric in order to work with filters. Underscore is allowed. Other symbols and whitespace are not allowed.
    */
  type AnnotationKey = Alphanumeric.NonEmptyWithUnderscoreNoWhitespace
  object AnnotationKey {
    private val InvalidCharRegex = "[^0-9a-zA-Z_]"

    def escape(raw: String): AnnotationKey =
      refineV[Alphanumeric.PNonEmptyWithUnderscoreNoWhitespace].unsafeFrom(raw.replaceAll(InvalidCharRegex, "_"))
  }

  type SegmentId = Hex._16
  object SegmentId
}
