package org.mkuthan.streamprocessing.infrastructure

import java.util.UUID

import com.spotify.scio.bigquery.types.BigQueryType

import com.softwaremill.diffx.ObjectMatcher
import com.softwaremill.diffx.SeqMatcher
import org.joda.time.Instant
import org.joda.time.LocalDate

import org.mkuthan.streamprocessing.infrastructure.bigquery.syntax.BigQueryTypesArbitrary
import org.mkuthan.streamprocessing.infrastructure.json.JsonSerde

trait IntegrationTestFixtures extends BigQueryTypesArbitrary {
  import IntegrationTestFixtures._

  implicit val sampleClassMatcher: SeqMatcher[SampleClass] = ObjectMatcher.seq[SampleClass].byValue(_.id)

  val SampleClassBigQueryType: BigQueryType[SampleClass] = BigQueryType[SampleClass]
  val SampleClassBigQuerySchema = SampleClassBigQueryType.schema

  val SampleObject1: SampleClass = SampleClass(
    id = UUID.randomUUID().toString,
    stringField = "complex 1",
    optionalStringField = Some("complex 1"),
    intField = 1,
    bigDecimalField = BigDecimal(1),
    instantField = Instant.parse("2014-09-10T12:03:01Z"),
    localDateField = LocalDate.parse("2014-09-10")
  )

  val SampleJson1: Array[Byte] = JsonSerde.writeJsonAsBytes(SampleObject1)

  val SampleObject2: SampleClass = SampleClass(
    id = UUID.randomUUID().toString,
    stringField = "complex 2",
    optionalStringField = None,
    intField = 2,
    bigDecimalField = BigDecimal(2),
    instantField = Instant.parse("2014-09-10T12:03:02Z"),
    localDateField = LocalDate.parse("2014-09-10")
  )

  val SampleJson2: Array[Byte] = JsonSerde.writeJsonAsBytes(SampleObject2)

  val SampleMap1: Map[String, String] = Map("key1" -> "value1")
  val SampleMap2: Map[String, String] = Map("key2" -> "value2")
}

object IntegrationTestFixtures {
  @BigQueryType.toTable
  final case class SampleClass(
      id: String,
      stringField: String,
      optionalStringField: Option[String],
      intField: Int,
      bigDecimalField: BigDecimal,
      instantField: Instant,
      localDateField: LocalDate
  )
}
