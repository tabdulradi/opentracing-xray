package com.abdulradi.opentracing.xray.tracing

import com.abdulradi.opentracing.xray.v1.model.TopLevelTrace

object `package` {
  type Result[T] = Either[String, T]
  type Extractor[S, T] = (S => Result[T])
  type SpanConverter[S] = Extractor[S, TopLevelTrace]
}

object Result {
  def fromNullable[T](t: T, msgIfEmpty: => String): Result[T] = Option(t).toRight(msgIfEmpty)
  def failure(msg: String): Result[Nothing] = Left(msg)
}