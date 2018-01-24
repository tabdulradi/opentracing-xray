package com.abdulradi.opentracing.xray.tracing

import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model.{SegmentId, TraceId}

private[tracing] object Parsers {
  import atto._
  import Atto._
  import Parser.{Failure, State, Success, TResult}
  import eu.timepit.refined.api.{Refined, Validate}

  type Result[T] = Either[String, T]

  implicit class RefinedOps[T](val parser: Parser[T]) extends AnyVal {

    import eu.timepit.refined.refineV

    def refined[P](implicit ev: Validate[T, P]): Parser[T Refined P] = {
      parser.flatMap(raw => new Parser[T Refined P] {
        override def apply[R](st0: State, kf: Failure[R], ks: Success[Refined[T, P], R]): TResult[R] =
          refineV(raw).fold(
            e => kf(st0, List.empty, e), // FIXME: is empty stack correct here?
            v => ks(st0, v)
          )
      })
    }
  }

  private val hexStr = stringOf(hexDigit)
  private val hex8 = hexStr.refined[Hex.P8]
  private val hex16 = hexStr.refined[Hex.P16]
  private val hex24 = hexStr.refined[Hex.P24]

  private val headerData = {
    val keyValuePair = pairBy(stringOf1(notChar('=')), char('='), stringOf1(notChar(';')))
    sepBy(keyValuePair, char(';')).map(_.toMap)
  }

  private val traceId: Parser[TraceId] =
    string("1-") ~> pairBy(hex8, char('-'), hex24).map(TraceId.tupled)

  private def parse[T](parser: Parser[T])(input: String): Result[T] = parser.parse(input).done.either

  val parseTracingHeader = parse(headerData) _
  val parseTraceId = parse(traceId) _
  val parseSegmentId = parse[SegmentId](hex16) _
  val parseSamplingDecision = parse(int) _
}
