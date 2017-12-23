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

package com.abdulradi.opentracing.xray.utils.refined

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.W

object Hex {
  type P8 = MatchesRegex[W.`"[0-9a-fA-F]{8}"`.T]
  type _8 = String Refined P8

  type P16 = MatchesRegex[W.`"[0-9a-fA-F]{16}"`.T]
  type _16 = String Refined P16

  type P24 = MatchesRegex[W.`"[0-9a-fA-F]{24}"`.T]
  type _24 = String Refined P24
}

object Alphanumeric {
  // Keys must be alphanumeric in order to work with filters. Underscore is allowed. Other symbols and whitespace are not allowed.
  type PNonEmptyWithUnderscoreNoWhitespace = MatchesRegex[W.`"[0-9a-fA-F0-9_]+"`.T]
  type NonEmptyWithUnderscoreNoWhitespace = String Refined PNonEmptyWithUnderscoreNoWhitespace
}
