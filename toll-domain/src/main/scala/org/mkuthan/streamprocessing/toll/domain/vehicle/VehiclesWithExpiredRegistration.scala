package org.mkuthan.streamprocessing.toll.domain.vehicle

import com.spotify.scio.bigquery.types.BigQueryType
import com.spotify.scio.values.SCollection
import com.spotify.scio.values.SideOutput
import com.spotify.scio.values.WindowOptions

import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime
import org.apache.beam.sdk.transforms.windowing.Repeatedly
import org.apache.beam.sdk.values.WindowingStrategy.AccumulationMode
import org.joda.time.Duration
import org.joda.time.Instant

import org.mkuthan.streamprocessing.shared.common.Message
import org.mkuthan.streamprocessing.shared.common.SumByKey
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntry
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothId
import org.mkuthan.streamprocessing.toll.domain.common.LicensePlate
import org.mkuthan.streamprocessing.toll.domain.registration.VehicleRegistration
import org.mkuthan.streamprocessing.toll.domain.registration.VehicleRegistrationId

final case class VehiclesWithExpiredRegistration(
    tollBoothId: TollBoothId,
    entryTime: Instant,
    licensePlate: LicensePlate,
    vehicleRegistrationId: VehicleRegistrationId
)

object VehiclesWithExpiredRegistration {

  @BigQueryType.toTable
  case class Raw(
      created_at: Instant,
      toll_booth_id: String,
      entry_time: Instant,
      license_plate: String,
      vehicle_registration_id: String
  )

  @BigQueryType.toTable
  case class Diagnostic(
      created_at: Instant,
      toll_booth_id: String,
      reason: String,
      count: Long = 1L
  ) {
    private lazy val keyFields = this match {
      case Diagnostic(created_at, toll_booth_id, reason, count @ _) =>
        Seq(created_at, toll_booth_id, reason)
    }
  }

  object Diagnostic {
    implicit val diagnostic: SumByKey[Diagnostic] =
      SumByKey.create(
        groupKeyFn = _.keyFields.mkString("|@|"),
        plusFn = (x, y) => x.copy(count = x.count + y.count)
      )
  }

  def calculateInFixedWindow(
      boothEntries: SCollection[TollBoothEntry],
      vehicleRegistrations: SCollection[VehicleRegistration],
      duration: Duration
  ): (SCollection[VehiclesWithExpiredRegistration], SCollection[Diagnostic]) = {
    val boothEntriesByLicensePlate = boothEntries
      .keyBy(_.licensePlate)
      .withFixedWindows(duration)

    val sideInputWindowOptions = WindowOptions(
      trigger = Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()),
      accumulationMode = AccumulationMode.DISCARDING_FIRED_PANES
    )

    val vehicleRegistrationByLicensePlate = vehicleRegistrations
      .keyBy(_.licensePlate)
      .withGlobalWindow(sideInputWindowOptions)

    val diagnostic = SideOutput[Diagnostic]()

    val (results, sideOutputs) = boothEntriesByLicensePlate
      .hashLeftOuterJoin(vehicleRegistrationByLicensePlate)
      .values
      .distinct
      .withSideOutputs(diagnostic)
      .flatMap {
        case ((boothEntry, Some(vehicleRegistration)), _) if vehicleRegistration.expired =>
          Some(toVehiclesWithExpiredRegistration(boothEntry, vehicleRegistration))
        case ((boothEntry, Some(vehicleRegistration)), ctx) if !vehicleRegistration.expired =>
          val createdAt = ctx.context.timestamp()
          val diagnosticReason = "Vehicle registration is not expired"
          ctx.output(diagnostic, toDiagnostic(createdAt, boothEntry, diagnosticReason))
          None
        case ((boothEntry, None), ctx) =>
          val createdAt = ctx.context.timestamp()
          val diagnosticReason = "Missing vehicle registration"
          ctx.output(diagnostic, toDiagnostic(createdAt, boothEntry, diagnosticReason))
          None
      }

    (results, sideOutputs(diagnostic))
  }

  def encode(input: SCollection[VehiclesWithExpiredRegistration]): SCollection[Message[Raw]] =
    input.withTimestamp.map { case (r, t) =>
      val payload = Raw(
        created_at = t,
        toll_booth_id = r.tollBoothId.id,
        entry_time = r.entryTime,
        license_plate = r.licensePlate.number,
        vehicle_registration_id = r.vehicleRegistrationId.id
      )
      Message(payload)
    }

  private def toVehiclesWithExpiredRegistration(
      boothEntry: TollBoothEntry,
      vehicleRegistration: VehicleRegistration
  ): VehiclesWithExpiredRegistration =
    VehiclesWithExpiredRegistration(
      tollBoothId = boothEntry.id,
      entryTime = boothEntry.entryTime,
      licensePlate = boothEntry.licensePlate,
      vehicleRegistrationId = vehicleRegistration.id
    )

  private def toDiagnostic(createdAt: Instant, boothEntry: TollBoothEntry, reason: String): Diagnostic =
    Diagnostic(
      created_at = createdAt,
      toll_booth_id = boothEntry.id.id,
      reason = reason
    )
}
