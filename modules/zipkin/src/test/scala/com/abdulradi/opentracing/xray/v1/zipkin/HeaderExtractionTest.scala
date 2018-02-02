package com.abdulradi.opentracing.xray.v1.zipkin

import scala.collection.JavaConverters._
import scala.collection.mutable

import brave.opentracing.BraveTracer
import brave.sampler.Sampler
import io.opentracing.SpanContext
import io.opentracing.propagation.{Format, TextMapExtractAdapter, TextMapInjectAdapter}
import org.scalatest._

class HeaderExtractionTest extends FunSuite {
  val converter = ZipkinSpanConverter.extractTopLevelTrace()((_, segmentExtractor) => segmentExtractor)
  val alwaysSampleTracer = TracerBuilder("", "", 0, Sampler.ALWAYS_SAMPLE, converter).build()
  val neverSampleTracer = TracerBuilder("", "", 0, Sampler.NEVER_SAMPLE, converter).build()

  def headers(pairs: (String, String)*) =
    new TextMapExtractAdapter(pairs.toMap.asJava)

  def getHeaders(tracer: BraveTracer, context: SpanContext) = {
    val carrier = new mutable.LinkedHashMap[String, String].asJava
    tracer.inject(context, Format.Builtin.HTTP_HEADERS, new TextMapInjectAdapter(carrier))
    carrier
  }

  def assertHeader(tracer: BraveTracer, input: String, expectedOutput: String) =
    checkHeaders(tracer, headers("x-amzn-trace-id" -> input))(hs =>
      assert(hs.apply("x-amzn-trace-id") === expectedOutput)
    )

  def checkHeaders(tracer: BraveTracer, headers: TextMapExtractAdapter)(assertions: Map[String, String] => Unit) = {
    val context = tracer.extract(Format.Builtin.HTTP_HEADERS, headers)
    assertions(getHeaders(tracer, context).asScala.toMap)
  }

  test("Empty headers means random generated headers, sampler decide on sampling field") {
    checkHeaders(alwaysSampleTracer, headers()) { hs =>
      val header = hs("x-amzn-trace-id")
      assert(header.length === 50)
      assert(header.startsWith("Root=1-") === true)
      assert(header(15) === '-')
      assert(header.endsWith(";Sampled=1") === true)
    }

    checkHeaders(neverSampleTracer, headers()) { hs =>
      val header = hs("x-amzn-trace-id")
      assert(header.length === 50)
      assert(header.startsWith("Root=1-") === true)
      assert(header(15) === '-')
      assert(header.endsWith(";Sampled=0") === true)
    }
  }

  test("No tracing header (non relevant headers) shouldn't affect extraction") {
    checkHeaders(alwaysSampleTracer, headers("foo" -> "bar")) { hs =>
      val header = hs("x-amzn-trace-id")
      assert(header.length === 50)
      assert(header.startsWith("Root=1-") === true)
      assert(header(15) === '-')
      assert(header.endsWith(";Sampled=1") === true)
    }
  }

  test("Root is the minimum") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Sampled=1"
    )
  }

  test("Max traceId should fit") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-ffffffff-ffffffffffffffffffffffff",
      "Root=1-ffffffff-ffffffffffffffffffffffff;Sampled=1"
    )
  }

  test("Lower case tracing header is still valid") {
    checkHeaders(alwaysSampleTracer, headers("x-amzn-trace-id" -> "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6")) { hs =>
     assert(hs("x-amzn-trace-id") === "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Sampled=1")
    }
  }

  test("Sampled should be parsed") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1"
    )
    assertHeader(
      neverSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0"
    )
  }

  test("Missing Sampled should be added") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1"
    )
    assertHeader(
      neverSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0"
    )
  }

  test("Optional Sampled should be parsed and added") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=?",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1"
    )
    assertHeader(
      neverSampleTracer,
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=?",
      "Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0"
    )
  }


  test("Parent should be parsed") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"
    )
  }

  test("Max Parent should fit") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-ffffffff-ffffffffffffffffffffffff;Parent=ffffffffffffffff;Sampled=1",
      "Root=1-ffffffff-ffffffffffffffffffffffff;Parent=ffffffffffffffff;Sampled=1"
    )
  }

  test("Root, Parent, Sampled combo") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"
    )
  }

  test("Root, Parent, not Sampled combo") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0"
    )
  }

  test("Root, Parent, might be Sampled combo") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"
    )
  }

  test("trailing simicolon shouldn't confuse the parser") {
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0;",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0"
    )
  }

  test("unknown keys should ignored for now") { // TODO: We should actually propagate them as is
    assertHeader(
      alwaysSampleTracer,
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Foo=Bar",
      "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"
    )
  }
}
