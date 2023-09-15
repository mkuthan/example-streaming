package org.mkuthan.streamprocessing.toll.application.streaming

import org.mkuthan.streamprocessing.infrastructure.common.IoDiagnostic
import org.mkuthan.streamprocessing.infrastructure.common.IoIdentifier
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothEntry
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothExit
import org.mkuthan.streamprocessing.toll.domain.booth.TollBoothStats
import org.mkuthan.streamprocessing.toll.domain.registration.VehicleRegistration
import org.mkuthan.streamprocessing.toll.domain.vehicle.TotalVehicleTime
import org.mkuthan.streamprocessing.toll.domain.vehicle.TotalVehicleTimeDiagnostic
import org.mkuthan.streamprocessing.toll.domain.vehicle.VehiclesWithExpiredRegistration
import org.mkuthan.streamprocessing.toll.domain.vehicle.VehiclesWithExpiredRegistrationDiagnostic

trait TollStreamingJobIo extends DiagnosticIo with RegistrationIo with TollBoothIo with VehicleIo

trait DiagnosticIo {
  val DiagnosticTableIoId: IoIdentifier[IoDiagnostic.Raw] =
    IoIdentifier[IoDiagnostic.Raw]("diagnostic-table-id")
}

trait RegistrationIo {
  val VehicleRegistrationTableIoId: IoIdentifier[VehicleRegistration.Raw] =
    IoIdentifier[VehicleRegistration.Raw]("toll.vehicle_registration")
  val VehicleRegistrationSubscriptionIoId: IoIdentifier[VehicleRegistration.Raw] =
    IoIdentifier[VehicleRegistration.Raw]("vehicle-registration-subscription-id")

  val VehicleRegistrationDlqBucketIoId: IoIdentifier[VehicleRegistration.DeadLetterRaw] =
    IoIdentifier[VehicleRegistration.DeadLetterRaw]("vehicle-registration-dlq-bucket-id")
}

trait TollBoothIo {
  val EntrySubscriptionIoId: IoIdentifier[TollBoothEntry.Payload] =
    IoIdentifier[TollBoothEntry.Payload]("entry-subscription-id")
  val EntryDlqBucketIoId: IoIdentifier[TollBoothEntry.DeadLetterPayload] =
    IoIdentifier[TollBoothEntry.DeadLetterPayload]("entry-dlq-bucket-id")

  val ExitSubscriptionIoId: IoIdentifier[TollBoothExit.Payload] =
    IoIdentifier[TollBoothExit.Payload]("exit-subscription-id")
  val ExitDlqBucketIoId: IoIdentifier[TollBoothExit.DeadLetterPayload] =
    IoIdentifier[TollBoothExit.DeadLetterPayload]("exit-dlq-bucket-id")

  val EntryStatsTableIoId: IoIdentifier[TollBoothStats.Raw] =
    IoIdentifier[TollBoothStats.Raw]("entry-stats-table-id")
}

trait VehicleIo {
  val VehiclesWithExpiredRegistrationTopicIoId: IoIdentifier[VehiclesWithExpiredRegistration.Raw] =
    IoIdentifier[VehiclesWithExpiredRegistration.Raw]("vehicles-with-expired-registration-topic-id")

  val VehiclesWithExpiredRegistrationDiagnosticTableIoId: IoIdentifier[VehiclesWithExpiredRegistrationDiagnostic.Raw] =
    IoIdentifier[VehiclesWithExpiredRegistrationDiagnostic.Raw](
      "vehicles-with-expired-registration-diagnostic-table-id"
    )

  val TotalVehicleTimeTableIoId: IoIdentifier[TotalVehicleTime.Raw] =
    IoIdentifier[TotalVehicleTime.Raw]("total-vehicle-time-table-id")

  val TotalVehicleTimeDiagnosticTableIoId: IoIdentifier[TotalVehicleTimeDiagnostic.Raw] =
    IoIdentifier[TotalVehicleTimeDiagnostic.Raw]("total-vehicle-time-diagnostic-table-id")
}
