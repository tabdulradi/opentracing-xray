package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.v1.model.TraceId

object CommonOps {
  implicit class TraceIdCommonOps(val underlying: TraceId) extends AnyVal {
    def toHeaderString: String =
      s"1-${underlying.originalRequestTime}-${underlying.identifier}"
  }
}
