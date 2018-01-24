package com.abdulradi.opentracing.xray.utils

import java.util.Map.Entry

object RichMap {
  implicit class RichIterableOfEntriesWrapper[K, V](val underlying: Iterable[Entry[K, V]]) extends AnyVal {
    def find(key: K): Option[V] =
      underlying.find(_.getKey == key).map(_.getValue)
  }
}
