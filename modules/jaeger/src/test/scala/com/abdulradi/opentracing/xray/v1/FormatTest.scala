package com.abdulradi.opentracing.xray.v1

import com.abdulradi.opentracing.xray.utils.refined.Alphanumeric.Path
import com.abdulradi.opentracing.xray.utils.refined.Hex
import com.abdulradi.opentracing.xray.v1.Format._
import com.abdulradi.opentracing.xray.v1.model._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string.{Uri, Url}
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.scalatest._


class FormatTest extends FunSuite {

  val json =
    """
      |{
      |  "name" : "example.com",
      |  "id" : "70de5b6f19ff9a0a",
      |  "start_time" : 1.478293361271E9,
      |  "trace_id" : "1-581cf771-a006649127e371903a2de979",
      |  "end_time" : 1.478293361449E9
      |}
    """.stripMargin


  // AWs
  val subSegmentAws = SubsegmentAws(Some("UpdateItem"), Some("account_id"), Some("us-west-2"),
    Some("UBQNSO5AEM8T4FDA4RQDEB94OVTDRVV4K4HIRGVJF66Q9ASUAAJG"), Some("queue_url"), Some("scorekeep-user"))
  val elb = ElasticBeanstalk(Some("scorekeep"), Some("app-5a56-170119_190650-stage-170119_190650"),
    Some(32))
  val ec2 = Ec2(Some("i-075ad396f12bc325a"), Some("us-west-2c"))
  val ecs = Ecs(Some("79c796ed2a7f864f485c76f83f3165488097279d296a7c05bd5201a1c69b2920"))
  val segmentAws = SegmentAws(None, Some(ecs), Some(ec2), elb)

  // Http
  val response = Response(Some(200), Some(-1))
  val url: String Refined Url = "http://www.example.com/api/user"
  val userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0"
  val crf = CommonRequestFields(Some("POST"), Some(url), Some(userAgent), Some("78.255.233.48"))
  val subSegmentRequest = SubsegmentRequest(crf, Some(true))
  val segmentRequest = SegmentRequest(crf, Some(true))
  val subSegmentHttp = DownstreamHttp(Some(subSegmentRequest), Some(response))
  val segmentHttp = ServedHttp(Some(segmentRequest), Some(response))

  // Sql
  val conUri: String Refined Uri = "jdbc:postgresql://aawijb5u25wdoy.cpamxznpdoq8.us-west-2.rds.amazonaws.com:5432/ebdb"
  val query = "SELECT  *  FROM  customers  WHERE  customer_id=?;"
  val sql = Sql(Some(""), Some(conUri), Some(query), Some("PostgreSQL"), Some("9.5.4"), Some("PostgreSQL 9.4.1211.jre7"),
    Some("dbuser"), Some(Statement))

  // Errors
  val stackFrame = StackFrame(Some("path"), Some(123), Some("label"))
  val hex_16: Hex._16 = "70de5b6f19ff9a0a"
  val exceptionDetails = ExceptionDetails(hex_16, Some("message"), Some("type"), Some(true), Some(123),
    Some(123), Some(hex_16), Seq(stackFrame))
  val path: Path = "/foo/bar/baz.log"
  val causeObject = CauseObject(path, Seq(path), Seq(exceptionDetails))
  val commonErrorFields = CommonErrorFields(true, true, false, ExceptionId(hex_16))


  // Segment
  val hex_8: Hex._8 = "58406520"
  val hex_24: Hex._24 = "a006649127e371903a2de979"
  val traceId = TraceId(hex_8, hex_24)
  val service = Service("version")
  val segmentId: SegmentId = "70de5b6f19ff9a0a"
  val subsegmentFields = SubsegmentFields(Some(Namespace.Aws), Seq(segmentId), Some(subSegmentHttp),
    Some(subSegmentAws), Some(sql))

  val annotations = Map(
    ("customer_category" -> 124),
    ("zip_code" -> 98101),
    ("country" -> "United States"),
    ("internal" -> false)
  )

  val metadata =
    """
      |"debug": {
      |      "test": "Metadata string from UserModel.saveUser"
      | }
    """.stripMargin
  //val metaParsed = parse(metadata).getOrElse(Json.Null)
  //  val commonFields = CommonFields(segmentId, "example.com", 1.478293361271E9,
  //    1.478293361449E9, Some(segmentHttp), Some(segmentAws), Some(commonErrorFields),
  //    annotations, metaParsed, Seq(Subsegment))
  //
  //  val topLevelFields = TopLevelFields()


  //
  //    val commonFields = CommonFields(SegmentId("70de5b6f19ff9a0a"), "example.com", 1.478293361271E9,
  //      1.478293361449E9, Some(), Some(), Some())
  //    val topLevelFields = TopLevelFields()

  // Test Sql model
  val sqlStr =
    """
      |{
      |    "connection_string" : "",
      |    "url": "jdbc:postgresql://aawijb5u25wdoy.cpamxznpdoq8.us-west-2.rds.amazonaws.com:5432/ebdb",
      |    "preparation": "statement",
      |    "database_type": "PostgreSQL",
      |    "database_version": "9.5.4",
      |    "driver_version": "PostgreSQL 9.4.1211.jre7",
      |    "user" : "dbuser",
      |    "sanitized_query" : "SELECT  *  FROM  customers  WHERE  customer_id=?;"
      |}
    """.stripMargin

  val jsonSql = sql.asJson
  val parsedSql = parse(sqlStr).getOrElse(Json.Null)
  test("Sql test") {
    assert(parsedSql === jsonSql)
  }

  // Test http model
  val httpStr =
    """
      |{
      |    "request": {
      |      "method": "POST",
      |      "client_ip": "78.255.233.48",
      |      "url": "http://www.example.com/api/user",
      |      "user_agent": "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0",
      |      "x_forwarded_for": true
      |    },
      |    "response": {
      |      "status": 200,
      |      "content_length" : -1
      |    }
      |  }
    """.stripMargin
  val jsonHttp = segmentHttp.asJson
  val parsedHttp = parse(httpStr).getOrElse(Json.Null)
  test("Http test") {
    assert(parsedHttp === jsonHttp)
  }

  // Test aws model
  val awsStr =
    """
      |{
      |  "elastic_beanstalk": {
      |    "version_label": "app-5a56-170119_190650-stage-170119_190650",
      |    "deployment_id": 32,
      |    "environment_name": "scorekeep"
      |  },
      |  "ec2": {
      |    "availability_zone": "us-west-2c",
      |    "instance_id": "i-075ad396f12bc325a"
      |  },
      |  "ecs": {
      |    "container": "79c796ed2a7f864f485c76f83f3165488097279d296a7c05bd5201a1c69b2920"
      |  }
      |}
    """.stripMargin
  val jsonAws = segmentAws.asJson.asObject.get
  val parsedAws = ("account_id" -> Json.Null) +: parse(awsStr).right.get.asObject.get
  test("SegmentAws test") {
    assert(parsedAws === jsonAws)
  }

}
