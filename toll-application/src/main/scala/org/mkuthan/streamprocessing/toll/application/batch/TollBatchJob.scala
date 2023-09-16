package org.mkuthan.streamprocessing.toll.application.batch

import com.spotify.scio.ContextAndArgs

import org.joda.time.Duration

import org.mkuthan.streamprocessing.infrastructure._
import org.mkuthan.streamprocessing.infrastructure.bigquery.RowRestriction
import org.mkuthan.streamprocessing.infrastructure.bigquery.StorageReadConfiguration
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntry
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothExit
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothStats
import org.mkuthan.streamprocessing.toll.domain.vehicle.TotalVehicleTime

object TollBatchJob extends TollBatchJobIo {

  private val OneHour = Duration.standardHours(1)

  private val OneDay = Duration.standardDays(1)

  def main(mainArgs: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(mainArgs)

    val config = TollBatchJobConfig.parse(args)

    // read toll booth entries and toll booth exists
    val boothEntryRecords = sc.readFromBigQuery(
      EntryTableIoId,
      config.entryTable,
      StorageReadConfiguration()
        .withRowRestriction(
          RowRestriction.DateColumnRestriction(TollBoothEntry.PartitioningColumnName, config.effectiveDate)
        )
    )
    val boothEntries = TollBoothEntry.decodeRecord(boothEntryRecords)

    val boothExitRecords = sc.readFromBigQuery(
      ExitTableIoId,
      config.exitTable,
      StorageReadConfiguration()
        .withRowRestriction(
          RowRestriction.DateColumnRestriction(TollBoothExit.PartitioningColumnName, config.effectiveDate)
        )
    )
    val boothExits = TollBoothExit.decodeRecord(boothExitRecords)

    // read vehicle registrations (TODO)

    // calculate tool booth stats
    val boothStatsHourly = TollBoothStats.calculateInFixedWindow(boothEntries, OneHour)
    TollBoothStats
      .encode(boothStatsHourly)
      .writeBoundedToBigQuery(EntryStatsHourlyTableIoId, config.entryStatsHourlyPartition)

    val boothStatsDaily = TollBoothStats.calculateInFixedWindow(boothEntries, OneDay)
    TollBoothStats
      .encode(boothStatsDaily)
      .writeBoundedToBigQuery(EntryStatsDailyTableIoId, config.entryStatsDailyPartition)

    // calculate total vehicle times
    val (totalVehicleTimes, _) =
      TotalVehicleTime.calculateInSessionWindow(boothEntries, boothExits, OneHour)
    TotalVehicleTime
      .encode(totalVehicleTimes)
      .writeBoundedToBigQuery(TotalVehicleTimeOneHourGapTableIoId, config.totalVehicleTimeOneHourGapPartition)

    val _ = sc.run()
  }
}
