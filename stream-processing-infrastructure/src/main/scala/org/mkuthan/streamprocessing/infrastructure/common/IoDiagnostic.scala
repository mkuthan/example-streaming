package org.mkuthan.streamprocessing.infrastructure.common

import com.spotify.scio.bigquery.types.BigQueryType
import com.spotify.scio.values.SCollection

import org.joda.time.Instant

import org.mkuthan.streamprocessing.shared.scio.syntax._
import org.mkuthan.streamprocessing.shared.scio.SumByKey

final case class IoDiagnostic(id: String, reason: String, count: Long = 1) {
  private lazy val keyFields = this match {
    case IoDiagnostic(id, reason, count @ _) =>
      Seq(id, reason)
  }
}

object IoDiagnostic {
  @BigQueryType.toTable
  final case class Record(created_at: Instant, id: String, reason: String, count: Long)

  implicit val sumByKey: SumByKey[IoDiagnostic] =
    SumByKey.create(
      keyFn = _.keyFields.mkString("|@|"),
      plusFn = (x, y) => x.copy(count = x.count + y.count)
    )

  def union(first: SCollection[IoDiagnostic], others: SCollection[IoDiagnostic]*): SCollection[IoDiagnostic] =
    first.unionInGlobalWindow(others: _*)

  def toRecord[T](diagnostic: IoDiagnostic, createdAt: Instant): Record =
    Record(
      created_at = createdAt,
      id = diagnostic.id,
      reason = diagnostic.reason,
      count = diagnostic.count
    )
}
