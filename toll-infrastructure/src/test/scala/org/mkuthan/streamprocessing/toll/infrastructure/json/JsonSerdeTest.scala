package org.mkuthan.streamprocessing.toll.infrastructure.json

import org.joda.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

final class JsonSerdeTest extends AnyFlatSpec with Matchers {

  import JsonSerdeTest._

  private val anySampleObject = Sample("a", 0, Instant.EPOCH)
  private val anySampleJson = """{"f1":"a","f2":0,"f3":0}"""

  behavior of "JsonSerde"

  it should "serialize sample object" in {
    JsonSerde.writeJsonAsString(anySampleObject) should be(anySampleJson)
  }

  it should "deserialize sample object" in {
    JsonSerde.readJsonFromString[Sample](anySampleJson) should be(anySampleObject)
  }
}

object JsonSerdeTest {
  // define all types used in domain
  final case class Sample(f1: String, f2: Int, f3: Instant)
}
