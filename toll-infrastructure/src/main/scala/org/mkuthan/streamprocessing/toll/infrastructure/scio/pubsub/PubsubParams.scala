package org.mkuthan.streamprocessing.toll.infrastructure.scio.pubsub

import scala.annotation.unused

import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO.Read
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO.Write

sealed trait PubsubReadParam {
  def configure[T](read: Read[T]): Read[T]
}

sealed trait PubsubWriteParam {
  def configure[T](write: Write[T]): Write[T]
}

case class PubsubReadConfiguration(
    idAttribute: IdAttribute = NoIdAttribute,
    tsAttribute: TimestampAttribute = NoTimestampAttribute
) {
  def withIdAttribute(idAttribute: IdAttribute): PubsubReadConfiguration =
    copy(idAttribute = idAttribute)

  def withTimestampAttribute(tsAttribute: TimestampAttribute): PubsubReadConfiguration =
    copy(tsAttribute = tsAttribute)

  def configure[T](read: Read[T]): Read[T] =
    params.foldLeft(read)((read, param) => param.configure(read))

  private lazy val params: Set[PubsubReadParam] = Set(
    idAttribute,
    tsAttribute
  )
}

case class PubsubWriteConfiguration(
    idAttribute: IdAttribute = NoIdAttribute,
    maxBatchBytesSize: MaxBatchBytesSize = MaxBatchBytesSize.Default,
    maxBatchSize: MaxBatchSize = MaxBatchSize.Default,
    tsAttribute: TimestampAttribute = NoTimestampAttribute
) {
  def withIdAttribute(idAttribute: IdAttribute): PubsubWriteConfiguration =
    copy(idAttribute = idAttribute)

  @unused("how to test batch bytes size?")
  def withMaxBatchBytesSize(maxBatchBytesSize: MaxBatchBytesSize): PubsubWriteConfiguration =
    copy(maxBatchBytesSize = maxBatchBytesSize)

  @unused("how to test batch size?")
  def withMaxBatchSize(maxBatchSize: MaxBatchSize): PubsubWriteConfiguration =
    copy(maxBatchSize = maxBatchSize)

  def withTimestampAttribute(tsAttribute: TimestampAttribute): PubsubWriteConfiguration =
    copy(tsAttribute = tsAttribute)

  def configure[T](write: Write[T]): Write[T] =
    params.foldLeft(write)((write, param) => param.configure(write))

  private lazy val params: Set[PubsubWriteParam] = Set(
    idAttribute,
    maxBatchBytesSize,
    maxBatchSize,
    tsAttribute
  )
}

sealed trait IdAttribute extends PubsubReadParam with PubsubWriteParam

case object NoIdAttribute extends IdAttribute {
  override def configure[T](read: Read[T]): Read[T] = read

  override def configure[T](write: Write[T]): Write[T] = write
}

case class NamedIdAttribute(name: String) extends IdAttribute {
  require(name.nonEmpty)

  override def configure[T](read: Read[T]): Read[T] =
    read.withIdAttribute(name)

  override def configure[T](write: Write[T]): Write[T] =
    write.withIdAttribute(name)
}

object NamedIdAttribute {
  val Default = NamedIdAttribute("id")
}

sealed trait TimestampAttribute extends PubsubReadParam with PubsubWriteParam

case object NoTimestampAttribute extends TimestampAttribute {
  override def configure[T](read: Read[T]): Read[T] = read

  override def configure[T](write: Write[T]): Write[T] = write
}

case class NamedTimestampAttribute(name: String) extends TimestampAttribute {
  require(name.nonEmpty)

  override def configure[T](read: Read[T]): Read[T] =
    read.withTimestampAttribute(name)

  override def configure[T](write: Write[T]): Write[T] =
    write.withTimestampAttribute(name)
}

object NamedTimestampAttribute {
  val Default = NamedTimestampAttribute("ts")
}

case class MaxBatchBytesSize(value: Int) extends PubsubWriteParam {
  require(value > 0)

  override def configure[T](write: Write[T]): Write[T] =
    write.withMaxBatchBytesSize(value)
}

object MaxBatchBytesSize {

  val Default = MaxBatchBytesSize(5 * 1024 * 1024)
}

case class MaxBatchSize(value: Int) extends PubsubWriteParam {
  require(value > 0)

  override def configure[T](write: Write[T]): Write[T] =
    write.withMaxBatchSize(value)
}

object MaxBatchSize {

  val Default = MaxBatchSize(100)
}
