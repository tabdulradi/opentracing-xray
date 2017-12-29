package com.abdulradi.opentracing.xray.utils

import java.util.Map.Entry

object RichMap {
  implicit class RichMapWrapper[K, V](val underlying: Map[K, V]) extends AnyVal {
    def getOrLeft[L](key: K, otherwise: => L): Either[L, V] =
      underlying.get(key).fold[Either[L, V]](Left(otherwise))(Right.apply)
  }

  implicit class RichIterableOfEntriesWrapper[K, V](val underlying: Iterable[Entry[K, V]]) extends AnyVal {
    def findOrLeft[L](key: K, otherwise: => L): Either[L, V] =
      underlying.find(_.getKey == key).fold[Either[L, V]](Left(otherwise))(entry => Right(entry.getValue))
  }
}
