package com.abdulradi.opentracing.xray.v1.zipkin

import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.refineV
import zipkin2.Span
import scala.collection.JavaConverters._

import io.circe.{Json, JsonObject}
import com.abdulradi.opentracing.xray.utils.RichOptionOfEither._
import cats.syntax.either._ // Needed to cross-compile to Scala 2.11

//
//  XRay: http://docs.aws.amazon.com/xray/latest/devguide/xray-concepts.html?shortFooter=true#xray-concepts-traces
//    - A (Request) Trace consists of Segments (Highlevel, probably one per microsevice per request)
//    - A Segment might consists of Subsegments (Can b granular to instrument specific functions or lines of code in your application or calling external services)
//
//  Terminology Mapping
//  Zipkin/Opentracing <----> XRay
//  Trace                     Trace
//  Span                      Segment
//  ???                       Subsegment
//

object `package` {
  import com.abdulradi.opentracing.xray.tracing
  type ZipkinSpanConverter = tracing.SpanConverter[Span]
  type Result[T] = tracing.Result[T]
  val Result = tracing.Result
}

object ZipkinSpanConverter {
  type Extractor[T] = com.abdulradi.opentracing.xray.tracing.Extractor[Span, T]
  final object Extractor {
    def success[T](f: Span => T): Extractor[T] = span => Right(f(span))
    final val empty: Extractor[Seq[Nothing]] = Extractor.success(_ => Seq.empty)
  }

  type OptionalExtractor[T] = Extractor[Option[T]]
  final object OptionalExtractor {
    final val none: OptionalExtractor[Nothing] = Extractor.success(_ => None)
  }

  private def microToSeconds(micro: Long): Double = micro / 1000000.0D

  def extractSpanId(span: Span): Result[Hex._16] =
    refineV[Hex.P16](span.id())

  def extractTraceId(span: Span): Result[TraceId] =
    if (span.traceId.length != 32) {
      Left(s"Span reported without a 128-bit trace ID. traceId=${span.traceId}")
    } else {
      val (originalRequestTime, identifier) = span.traceId.splitAt(8)
      for {
        time <- refineV[Hex.P8](originalRequestTime)
        id <- refineV[Hex.P24](identifier)
      } yield TraceId(time, id)
    }

  type StartAndEndTime = (Double, Option[Double])
  def extractStartAndEndTime(span: Span): Result[StartAndEndTime] = for {
    startTimeMicro <- Result.fromNullable(span.timestamp, "span.timestamp == null")
    startTime = microToSeconds(startTimeMicro)
    maybeEndtime = Option(span.duration).map(d => microToSeconds(d) + startTime)
  } yield (startTime, maybeEndtime)

  type MetadataAndAnnotations = (JsonObject, Map[AnnotationKey, AnnotationValue])
  def extractMetadataAndAnnotations(keysToDropFromTags: Set[String] = Set.empty): Extractor[MetadataAndAnnotations] = { span =>
  val (bigTags, smallTags) =
      (span.tags().asScala.toMap -- keysToDropFromTags)
        .partition { case (key, value) => key.length > 250 || value.length > 250 }

    val (bigAnnotations, smallAnnotation) =
      span.annotations().asScala.map(a => a.timestamp().toString -> a.value()).partition(_._2.length > 250)

    val annotations: Map[AnnotationKey, AnnotationValue] =
      (smallTags ++ smallAnnotation).map {
        case (key, value) => AnnotationKey.escape(key) -> StringAnnotation(value)
      }

    val metadata: JsonObject =
      JsonObject.fromMap(
        (bigTags ++ bigAnnotations).map {
          case (key, value) => key -> Json.fromString(value)
        }
      )

    Right((metadata, annotations))
  }

  def extractSegmentName(span: Span): Result[String] =
    Result.fromNullable(span.localServiceName, "span.localServiceName == null")

  def extractSubsegmentName(span: Span): Result[String] =
    Result.fromNullable(span.name, "span.name == null")

  def extractSegmentParentId(span: Span): Result[Option[Hex._16]] =
    Option(span.parentId())
      .filterNot("0000000000000000".equals) // XRayExtractor sets this dummy value in case there is no parent
      .map(id => refineV[Hex.P16](id))
      .toEitherOfOption

  def extractSubsegmentParentId(span: Span): Result[Hex._16] =
    extractSegmentParentId(span)
      .flatMap(_.fold(s"Unexpected subsegment parentId == ${span.parentId}".asLeft[Hex._16])(_.asRight))

  def extractCommonFields(
    spanIdExtractor: Extractor[Hex._16] = extractSpanId,
    nameExtractor: Extractor[String] = extractSegmentName,
    startAndEndTimeExtractor: Extractor[StartAndEndTime] = extractStartAndEndTime,
    errorExtractor: OptionalExtractor[CommonErrorFields] = OptionalExtractor.none,
    metadataAndAnnotationsExtractor: Extractor[MetadataAndAnnotations] = extractMetadataAndAnnotations(),
    subsegmentsExtractor: Extractor[Seq[Subsegment]] = Extractor.empty
  ): Extractor[CommonFields] = span => for {
    id <- spanIdExtractor(span)
    name <- nameExtractor(span)
    startAndEndTime <- startAndEndTimeExtractor(span)
    error <- errorExtractor(span)
    metadataAndAnnotations <- metadataAndAnnotationsExtractor(span)
    subsegments <- subsegmentsExtractor(span)
  } yield CommonFields(id, name, startAndEndTime._1, startAndEndTime._2, error, metadataAndAnnotations._2, Some(metadataAndAnnotations._1), subsegments)

  def extractTopLevelFields(
    commonFieldsExtractor: Extractor[CommonFields] = extractCommonFields(),
    traceIdExtractor: Extractor[TraceId] = extractTraceId
  ): Extractor[TopLevelFields] = span => for {
    common <- commonFieldsExtractor(span)
    traceId <- traceIdExtractor(span)
  } yield TopLevelFields(common, traceId)

  def extractSegment(
    topLevelFieldsExtractor: Extractor[TopLevelFields] = extractTopLevelFields(),
    serviceExtractor: OptionalExtractor[Service] = OptionalExtractor.none,
    userExtractor: OptionalExtractor[String] = OptionalExtractor.none,
    originExtractor: OptionalExtractor[Origin] = OptionalExtractor.none,
    parentIdExtractor: OptionalExtractor[Hex._16] = extractSegmentParentId,
    servedHttpExtractor: OptionalExtractor[ServedHttp] = OptionalExtractor.none,
    segmentAwsExtractor: OptionalExtractor[SegmentAws] = OptionalExtractor.none
  ): Extractor[Segment] = span => for {
    topLevelFields <- topLevelFieldsExtractor(span)
    service <- serviceExtractor(span)
    user <- userExtractor(span)
    origin <- originExtractor(span)
    parentId <- parentIdExtractor(span)
    http <- servedHttpExtractor(span)
    aws <- segmentAwsExtractor(span)
  } yield Segment(topLevelFields, service, user, origin, parentId, http, aws)

  def extractSubsegmentFields(
    namespaceExtractor: OptionalExtractor[Namespace] = OptionalExtractor.none,
    precursorIdsExtractor: Extractor[Seq[SegmentId]] = Extractor.empty,
    httpExtractor: OptionalExtractor[DownstreamHttp] = OptionalExtractor.none,
    awsExtractor: OptionalExtractor[SubsegmentAws] = OptionalExtractor.none,
    sqlExtractor: OptionalExtractor[Sql] = OptionalExtractor.none
  ): Extractor[SubsegmentFields] = span => for {
    namespace <- namespaceExtractor(span)
    precursorIds <- precursorIdsExtractor(span)
    http <- httpExtractor(span)
    aws <- awsExtractor(span)
    sql <- sqlExtractor(span)
  } yield SubsegmentFields(namespace, precursorIds, http, aws, sql)

  def extractIndependentSubsegment(
    topLevelFieldsExtractor: Extractor[TopLevelFields] = extractTopLevelFields(extractCommonFields(nameExtractor = extractSubsegmentName)),
    subsegmentFieldsExtractor: Extractor[SubsegmentFields] = extractSubsegmentFields(),
    parentIdExtractor: Extractor[SegmentId] = extractSubsegmentParentId
  ): Extractor[IndependentSubsegment] = span => for {
    topLevelFields <- topLevelFieldsExtractor(span)
    subsegmentFields <- subsegmentFieldsExtractor(span)
    parentId <- parentIdExtractor(span)
  } yield IndependentSubsegment(topLevelFields, subsegmentFields, parentId)

  def extractTopLevelTrace(
    extractIfSubSegment: Extractor[IndependentSubsegment] = extractIndependentSubsegment(),
    extractIfSegment: Extractor[Segment] = extractSegment()
  )(
    decider: (Extractor[IndependentSubsegment], Extractor[Segment]) => Extractor[TopLevelTrace]
  ): ZipkinSpanConverter = decider(extractIfSubSegment, extractIfSegment)
}