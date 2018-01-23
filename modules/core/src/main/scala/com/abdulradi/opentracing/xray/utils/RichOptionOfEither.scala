package com.abdulradi.opentracing.xray.utils

object RichOptionOfEither {
  implicit class RichOptionOfEitherWrapper[L, R](val underlying: Option[Either[L, R]]) extends AnyVal {
    def toEitherOfOption: Either[L, Option[R]] =
      underlying match {
        case Some(Right(right)) => Right(Some(right))
        case Some(Left(left)) => Left(left)
        case None => Right(None)
      }
  }
}
