package org.mkuthan.streamprocessing.toll.infrastructure.scio

import scala.language.implicitConversions

import com.spotify.scio.coders.Coder
import com.spotify.scio.values.SCollection

import org.apache.beam.sdk.io.TextIO

import org.mkuthan.streamprocessing.toll.infrastructure.json.JsonSerde

final class StorageSCollectionOps[T <: AnyRef](private val self: SCollection[T]) extends AnyVal {
  def saveToStorage(
      location: StorageLocation[T]
  )(implicit c: Coder[T]): Unit = {
    val io = TextIO
      .write()
      .withWindowedWrites()
      .to(location.path)

    self
      .map(JsonSerde.write)
      .saveAsCustomOutput("SaveToStorage", io)
  }

}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
trait SCollectionStorageSyntax {
  implicit def storageSCollectionOps[T <: AnyRef](sc: SCollection[T]): StorageSCollectionOps[T] =
    new StorageSCollectionOps(sc)
}
