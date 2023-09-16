package org.mkuthan.streamprocessing.toll.application.batch

import com.spotify.scio.io.CustomIO
import com.spotify.scio.testing.JobTest

import org.joda.time.Instant
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.mkuthan.streamprocessing.test.scio.JobTestScioContext
import org.mkuthan.streamprocessing.toll.application.TollJobFixtures
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntry
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothExit
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothStats
import org.mkuthan.streamprocessing.toll.domain.vehicle.TotalVehicleTime

class TollBatchJobTest extends AnyFlatSpec with Matchers
    with JobTestScioContext
    with TollJobFixtures
    with TollBatchJobIo {

  "Toll job" should "run in the batch mode" in {
    JobTest[TollBatchJob.type]
      .args(
        "--effectiveDate=2023-09-15",
        "--entryTable=toll.entry",
        "--exitTable=toll.exit",
        "--entryStatsHourlyTable=toll.entry_stats_hourly",
        "--entryStatsDailyTable=toll.entry_stats_daily",
        "--totalVehicleTimeOneHourGapTable=toll.total_vehicle_time_one_hour_gap"
      )
      // read toll booth entries and toll booth exists
      .input(CustomIO[TollBoothEntry.Record](EntryTableIoId.id), Seq(anyTollBoothEntryRecord))
      .input(CustomIO[TollBoothExit.Record](ExitTableIoId.id), Seq(anyTollBoothExitRecord))
      // calculate tool booth stats
      .output(CustomIO[TollBoothStats.Record](EntryStatsHourlyTableIoId.id)) { results =>
        results should containSingleValue(
          anyTollBoothStatsRecord.copy(created_at = Instant.parse("2014-09-10T12:59:59.999Z"))
        )
      }
      .output(CustomIO[TollBoothStats.Record](EntryStatsDailyTableIoId.id)) { results =>
        results should containSingleValue(
          anyTollBoothStatsRecord.copy(created_at = Instant.parse("2014-09-10T23:59:59.999Z"))
        )
      }
      // calculate total vehicle times
      .output(CustomIO[TotalVehicleTime.Record](TotalVehicleTimeOneHourGapTableIoId.id)) { results =>
        results should containSingleValue(
          anyTotalVehicleTimeRecord.copy(created_at = Instant.parse("2014-09-10T13:02:59.999Z"))
        )
      }
      .run()
  }
}
