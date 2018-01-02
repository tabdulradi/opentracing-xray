package com.abdulradi.opentracing.xray.utils

import scala.util.{Failure, Success, Try}

object RichTry {
  implicit class RichTryWrapper[T](underlying: Try[T]) {
    def fold[U](fa: Throwable => U, fb: T => U): U =
      underlying match {
        case Failure(a) => fa(a)
        case Success(b) => fb(b)
      }
  }
}
