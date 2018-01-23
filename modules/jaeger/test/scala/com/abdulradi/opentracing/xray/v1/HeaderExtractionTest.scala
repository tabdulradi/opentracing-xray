package com.abdulradi.opentracing.xray.v1

import scala.collection.JavaConverters._

import com.abdulradi.opentracing.xray.v1.jaeger.XRayTextMapCodec
import com.abdulradi.opentracing.xray.v1.model.TracingHeader
import com.uber.jaeger.samplers.{ConstSampler, Sampler, SamplingStatus}
import io.opentracing.propagation.TextMapExtractAdapter
import org.scalatest._

class HeaderExtractionTest extends FunSuite {
  import com.abdulradi.opentracing.xray.v1.jaeger.ConversionOps._

  val codec = new XRayTextMapCodec(new ConstSampler(true))
  val nonSamplerCodec = new XRayTextMapCodec(new ConstSampler(false))

  val codecNoSampler = new XRayTextMapCodec(new Sampler { // Fails if methods are called
    override def sample(operation: String, id: Long): SamplingStatus = ???
    override def close(): Unit = ???
  })

  def headers(pairs: (String, String)*) = new TextMapExtractAdapter(pairs.toMap.asJava)
  def traceHeader(value: String) = headers("X-Amzn-Trace-Id" -> value)
  val flagNotSampled: Byte = 0
  val flagSampled: Byte = 1

  test("Empty headers means random generated headers, sampler decide on sampling field") {
    val context = codec.extract(headers())
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(context.getTraceId === 0) // Dummy traceId, since XRay's won't fit a Long
    assert(header.parentSegmentId === None)
    assert(context.getFlags === flagSampled)
    assert(header.samplingDecision === Some(1))

    val context2 = nonSamplerCodec.extract(headers())
    val header2 = TracingHeader.fromSpanContext(context2).right.get
    assert(context2.getFlags === flagNotSampled)
    assert(header2.samplingDecision === Some(0))
  }

  test("No tracing header (non relevant headers) shouldn't affect extraction") {
    val context = codec.extract(headers("foo" -> "bar"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(context.getTraceId === 0) // Dummy traceId, since XRay's won't fit a Long
    assert(header.parentSegmentId === None)
    assert(context.getFlags === flagSampled)
    assert(header.samplingDecision === Some(1))
  }

  test("Root is the minimum, sampler decides whether to sample or not") {
    val context = codec.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(context.getTraceId === 0) // Dummy traceId, since XRay's won't fit a Long
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
    assert(header.parentSegmentId === None)
    assert(header.samplingDecision === Some(1))

    val context2 = nonSamplerCodec.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6"))
    val header2 = TracingHeader.fromSpanContext(context2).right.get
    assert(context2.getFlags === flagNotSampled)
    assert(header2.samplingDecision === Some(0))
  }

  test("Lower case tracing header is still valid") {
    val context = codec.extract(headers("x-amzn-trace-id" -> "Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }

  test("Sampled should be obeyed") {
    val context1 = codecNoSampler.extract(traceHeader("Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=1"))
    val header1 = TracingHeader.fromSpanContext(context1).right.get
    assert(header1.samplingDecision === Some(1))
    assert(header1.rootTraceId.originalRequestTime.value === "5759e988")
    assert(header1.rootTraceId.identifier.value === "bd862e3fe1be46a994272793")

    val context2 = codecNoSampler.extract(traceHeader("Root=1-5759e988-bd862e3fe1be46a994272793;Sampled=0"))
    val header2 = TracingHeader.fromSpanContext(context2).right.get
    assert(header2.samplingDecision === Some(0))
    assert(header2.rootTraceId.originalRequestTime.value === "5759e988")
    assert(header2.rootTraceId.identifier.value === "bd862e3fe1be46a994272793")
  }

  test("Parent should be parsed") {
    val context = codec.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(header.parentSegmentId.get.value === "6a3dc251f20785d7")
    assert(header.samplingDecision === Some(1)) // Test codec always samples if no Sampled header
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }

  test("Root, Parent, Sampled combo") {
    val context = codecNoSampler.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=1"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(header.parentSegmentId.get.value === "6a3dc251f20785d7")
    assert(header.samplingDecision === Some(1))
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }

  test("Root, Parent, not Sampled combo") {
    val context = codecNoSampler.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Sampled=0"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(header.parentSegmentId.get.value === "6a3dc251f20785d7")
    assert(header.samplingDecision === Some(0))
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }

  test("trailing simicolon shouldn't confuse the parser") {
    val context = codec.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(context.getTraceId === 0) // Dummy traceId, since XRay's won't fit a Long
    assert(header.parentSegmentId.get.value === "6a3dc251f20785d7")
    assert(header.samplingDecision === Some(1))
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }

  test("unknown keys should ignored") {
    val context = codec.extract(traceHeader("Root=1-5a5cd12f-f08cdaba788bc0a0f88f72e6;Parent=6a3dc251f20785d7;Foo=Bar"))
    val header = TracingHeader.fromSpanContext(context).right.get
    assert(context.getTraceId === 0) // Dummy traceId, since XRay's won't fit a Long
    assert(header.parentSegmentId.get.value === "6a3dc251f20785d7")
    assert(header.samplingDecision === Some(1))
    assert(header.rootTraceId.originalRequestTime.value === "5a5cd12f")
    assert(header.rootTraceId.identifier.value === "f08cdaba788bc0a0f88f72e6")
  }
}
