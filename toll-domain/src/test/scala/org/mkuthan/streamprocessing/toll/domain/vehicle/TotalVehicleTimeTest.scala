package org.mkuthan.streamprocessing.toll.domain.vehicle

import org.joda.time.Duration
import org.joda.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.mkuthan.streamprocessing.test.scio._
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntry
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntryFixture
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothExit
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothExitFixture
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothId
import org.mkuthan.streamprocessing.toll.domain.common.LicensePlate

class TotalVehicleTimeTest extends AnyFlatSpec with Matchers
    with TestScioContext
    with TollBoothEntryFixture
    with TollBoothExitFixture
    with TotalVehicleTimeFixture {

  import TotalVehicleTime._

  private val FiveMinutes = Duration.standardMinutes(5)

  behavior of "TotalVehicleTime"

  it should "calculate TotalVehicleTime using session window" in runWithScioContext { sc =>
    val tollBoothId = TollBoothId("1")
    val licensePlate = LicensePlate("AB 123")
    val entryTime = Instant.parse("2014-09-10T12:03:01Z")
    val exitTime = Instant.parse("2014-09-10T12:04:03Z")

    val tollBoothEntry = anyTollBoothEntry.copy(id = tollBoothId, licensePlate = licensePlate, entryTime = entryTime)
    val tollBoothExit = anyTollBoothExit.copy(id = tollBoothId, licensePlate = licensePlate, exitTime = exitTime)

    val boothEntries = boundedTestCollectionOf[TollBoothEntry]
      .addElementsAtTime(tollBoothEntry.entryTime, tollBoothEntry)
      .build()

    val boothExits = boundedTestCollectionOf[TollBoothExit]
      .addElementsAtTime(tollBoothExit.exitTime, tollBoothExit)
      .build()

    val (results, diagnostic) =
      calculateInSessionWindow(sc.testBounded(boothEntries), sc.testBounded(boothExits), FiveMinutes)

    results.withTimestamp should inOnTimePane("2014-09-10T12:03:01Z", "2014-09-10T12:09:03Z") {
      containSingleValueAtTime(
        "2014-09-10T12:09:02.999Z",
        anyTotalVehicleTime.copy(
          tollBoothId = tollBoothId,
          licensePlate = licensePlate,
          entryTime = entryTime,
          exitTime = exitTime,
          duration = Duration.standardSeconds(62)
        )
      )
    }

    diagnostic should beEmpty
  }

  it should "emit diagnostic if TollBoothExit is after session window gap" in runWithScioContext { sc =>
    val tollBoothId = TollBoothId("1")
    val licensePlate = LicensePlate("AB 123")
    val entryTime = Instant.parse("2014-09-10T12:03:01Z")
    val exitTime = Instant.parse("2014-09-10T12:08:03Z")

    val tollBoothEntry = anyTollBoothEntry.copy(id = tollBoothId, licensePlate = licensePlate, entryTime = entryTime)
    val tollBoothExit = anyTollBoothExit.copy(id = tollBoothId, licensePlate = licensePlate, exitTime = exitTime)

    val boothEntries = boundedTestCollectionOf[TollBoothEntry]
      .addElementsAtTime(tollBoothEntry.entryTime, tollBoothEntry)
      .build()

    val boothExits = boundedTestCollectionOf[TollBoothExit]
      .addElementsAtTime(tollBoothExit.exitTime, tollBoothExit)
      .build()

    val (results, diagnostic) =
      calculateInSessionWindow(sc.testBounded(boothEntries), sc.testBounded(boothExits), FiveMinutes)

    results should beEmpty

    diagnostic.withTimestamp should inOnTimePane("2014-09-10T12:03:01Z", "2014-09-10T12:08:01Z") {
      containSingleValueAtTime(
        "2014-09-10T12:08:00.999Z",
        TotalVehicleTimeDiagnostic(tollBoothId, "Missing TollBoothExit to calculate TotalVehicleTime", 1)
      )
    }
  }

  it should "calculate TotalVehicleTime using global window" in runWithScioContext { sc =>
    val tollBoothId = TollBoothId("1")
    val licensePlate = LicensePlate("AB 123")
    val entryTime = Instant.parse("2014-09-10T12:03:01Z")
    val exitTime = Instant.parse("2014-09-10T12:04:03Z")

    val tollBoothEntry = anyTollBoothEntry.copy(id = tollBoothId, licensePlate = licensePlate, entryTime = entryTime)
    val tollBoothExit = anyTollBoothExit.copy(id = tollBoothId, licensePlate = licensePlate, exitTime = exitTime)

    val boothEntries = boundedTestCollectionOf[TollBoothEntry]
      .addElementsAtMinimumTime(tollBoothEntry)
      .build()

    val boothExits = boundedTestCollectionOf[TollBoothExit]
      .addElementsAtMinimumTime(tollBoothExit)
      .build()

    val (results, diagnostic) =
      calculateInGlobalWindow(sc.testBounded(boothEntries), sc.testBounded(boothExits))

    results should containSingleValue(
      anyTotalVehicleTime.copy(
        tollBoothId = tollBoothId,
        licensePlate = licensePlate,
        entryTime = entryTime,
        exitTime = exitTime,
        duration = Duration.standardSeconds(62)
      )
    )

    diagnostic should beEmpty
  }

  it should "encode into record" in runWithScioContext { sc =>
    val recordTimestamp = Instant.parse("2014-09-10T12:08:00.999Z")
    val inputs = boundedTestCollectionOf[TotalVehicleTime]
      .addElementsAtTime(recordTimestamp, anyTotalVehicleTime)
      .build()

    val results = encode(sc.testBounded(inputs))
    results should containSingleValue(anyTotalVehicleTimeRecord.copy(record_timestamp = recordTimestamp))
  }
}
