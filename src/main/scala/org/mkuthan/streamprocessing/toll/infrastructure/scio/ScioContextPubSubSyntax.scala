package org.mkuthan.streamprocessing.toll.infrastructure.scio

import scala.language.implicitConversions

import com.spotify.scio.coders.Coder
import com.spotify.scio.values.SCollection
import com.spotify.scio.ScioContext

import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO

import org.mkuthan.streamprocessing.toll.infrastructure.json.JsonSerde

final class PubSubScioContextOps(private val self: ScioContext) extends AnyVal {
  def subscribeToPubSub[T <: AnyRef: Coder: Manifest](
      subscription: PubSubSubscription[T]
  ): SCollection[T] = {
    val io = PubsubIO
      .readStrings()
      .fromSubscription(subscription.id)
    self
      .customInput(s"SubscribeToPubSub", io)
      .map(JsonSerde.read[T])
  }
}

@SuppressWarnings(Array("org.wartremover.warts.ImplicitConversion"))
trait ScioContextPubSubSyntax {
  implicit def pubSubScioContextOps(sc: ScioContext): PubSubScioContextOps = new PubSubScioContextOps(sc)
}
