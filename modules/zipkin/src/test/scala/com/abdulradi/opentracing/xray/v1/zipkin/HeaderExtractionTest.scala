package com.abdulradi.opentracing.xray.v1.zipkin

import scala.collection.JavaConverters._
import scala.collection.mutable

import brave.opentracing.BraveTracer
import brave.sampler.Sampler
import io.opentracing.SpanContext
import io.opentracing.propagation.{Format, TextMapExtractAdapter, TextMapInjectAdapter}
import org.scalatest._

class HeaderExtractionTest extends FunSuite {
  val tracer = TracerBuilder("", "", 0, Sampler.ALWAYS_SAMPLE).build()

  def headers(pairs: (String, String)*) =
    new TextMapExtractAdapter(pairs.toMap.asJava)

  def getHeader(tracer: BraveTracer, context: SpanContext): String = {
    val carrier = new mutable.LinkedHashMap[String, String].asJava
    tracer.inject(context, Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(carrier))
    carrier.get("x-amzn-trace-id")
  }

  def assertHeader(input: String, expectedOutput: String) = {
    val context = tracer.extract(Format.Builtin.HTTP_HEADERS, headers("x-amzn-trace-id" -> input))
    assert(getHeader(tracer, context) === expectedOutput)
  }

  test("Empty headers means random generated headers, sampler decide on sampling field") {
    val context = tracer.extract(Format.Builtin.HTTP_HEADERS, headers())
    val header = getHeader(tracer, context)
    assert(header.length === 50)
    assert(header.startsWith("Root=1-") === true)
    assert(header(15) === '-')
    assert(header.endsWith(";Sampled=?") === true)
  }

  test("No tracing header (non relevant headers) shouldn't affect extraction") {
    val context = tracer.extract(Format.Builtin.HTTP_HEADERS, headers("foo" -> "bar"))
    val header = getHeader(tracer, context)
    assert(header.length === 50)
    assert(header.startsWith("Root=1-") === true)
    assert(header(15) === '-')
    assert(header.endsWith(";Sampled=?") === true)
  }

  test("Root is the minimum") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Sampled=?"
    )
  }

  test("Max traceId should fit") {
    assertHeader(
      "Root=1-ffffffff-ffffffffffffffffffffffff",
      "Root=1-ffffffff-ffffffffffffffffffffffff;Sampled=?"
    )
  }

  test("Lower case tracing header is still valid") {
    val context = tracer.extract(Format.Builtin.HTTP_HEADERS, headers("x-amzn-trace-id" -> "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6"))
    assert(getHeader(tracer, context) === "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Sampled=?")
  }

  test("Sampled should be parsed") {
    assertHeader(
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1"
    )
  }

  test("Parent should be parsed") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=?"
    )
  }

  test("Max Parent should fit") {
    assertHeader(
      "Root=1-ffffffff-ffffffffffffffffffffffff;Parent=ffffffffffffffff;Sampled=1",
      "Root=1-ffffffff-ffffffffffffffffffffffff;Parent=ffffffffffffffff;Sampled=1"
    )
  }

  test("Root, Parent, Sampled combo") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"
    )
  }

  test("Root, Parent, not Sampled combo") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0"
    )
  }

  test("Root, Parent, might be Sampled combo") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=?",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=?"
    )
  }

  test("trailing simicolon shouldn't confuse the parser") {
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0;",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0"
    )
  }

  test("unknown keys should ignored for now") { // TODO: We should actually propagate them as is
    assertHeader(
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Foo=Bar",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=?"
    )
  }
}
