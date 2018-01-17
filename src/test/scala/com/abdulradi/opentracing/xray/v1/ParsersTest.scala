package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.v1.model.TraceId
import eu.timepit.refined.auto._
import org.scalatest._

class ParsersTest extends FunSuite {
  import com.abdulradi.opentracing.xray.v1.jaeger.Parsers._

  test("Simple header") {
    val data = parseTracingHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6")
    assert(data === Right(Map("Root" -> "1-5a5cd12f-f08cdaba788bc0a0f88f72e6")))
  }

  test("Complex header") {
    val data = parseTracingHeader("Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1")
    assert(data === Right(Map(
      "Root" -> "1-5759e988-bd862e3fe1be46a994272793",
      "Sampled" -> "1"
    )))
  }

  test("Trace Id") {
    val traceId = parseTraceId("1-5a5cd12f-f08cdaba788bc0a0f88f72e6")
    assert(traceId === Right(TraceId("5a5cd12f", "f08cdaba788bc0a0f88f72e6")))
  }


}
