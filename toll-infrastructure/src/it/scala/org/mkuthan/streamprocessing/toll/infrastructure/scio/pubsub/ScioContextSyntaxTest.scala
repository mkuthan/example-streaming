package org.mkuthan.streamprocessing.toll.infrastructure.scio.pubsub

import org.joda.time.Instant
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.LoneElement._

import org.mkuthan.streamprocessing.shared.it.client.PubSubClient._
import org.mkuthan.streamprocessing.shared.it.common.IntegrationTestPatience
import org.mkuthan.streamprocessing.shared.it.common.RandomString.randomString
import org.mkuthan.streamprocessing.shared.it.context.ItScioContext
import org.mkuthan.streamprocessing.shared.it.context.PubsubContext
import org.mkuthan.streamprocessing.shared.it.sink.InMemorySink
import org.mkuthan.streamprocessing.toll.infrastructure.scio._

class ScioContextSyntaxTest extends AnyFlatSpec
    with Matchers
    with Eventually
    with IntegrationTestPatience
    with ItScioContext
    with PubsubContext {

  import IntegrationTestFixtures._

  behavior of "Pubsub ScioContext syntax"

  it should "subscribe JSON" in withScioContext { implicit sc =>
    options.setBlockOnRun(false)

    withTopic[SampleClass] { topic =>
      withSubscription[SampleClass](topic.id) { subscription =>
        publishMessages(
          topic.id,
          (SampleJson1, SampleMap1),
          (SampleJson2, SampleMap2)
        )

        val (messages, _) = sc.subscribeJsonFromPubsub(subscription)

        val sink = InMemorySink(messages)

        val run = sc.run()

        eventually {
          sink.toSeq should contain.only(
            PubsubMessage(SampleObject1, SampleMap1),
            PubsubMessage(SampleObject2, SampleMap2)
          )
        }

        run.pipelineResult.cancel()
      }
    }
  }

  it should "subscribe invalid JSON and put into DLQ" in withScioContext { implicit sc =>
    options.setBlockOnRun(false)

    withTopic[SampleClass] { topic =>
      withSubscription[SampleClass](topic.id) { subscription =>
        publishMessages(topic.id, (InvalidJson, SampleMap1))

        val (_, dlq) = sc.subscribeJsonFromPubsub(subscription)

        val sink = InMemorySink(dlq)

        val run = sc.run()

        eventually {
          val error = sink.toElement
          error.payload should be(InvalidJson)
          error.attributes should be(SampleMap1)
          error.error should startWith("Unrecognized token 'invalid'")
        }

        run.pipelineResult.cancel()
      }
    }
  }

  it should "subscribe JSON with id attribute" in withScioContext { implicit sc =>
    options.setBlockOnRun(false)

    withTopic[SampleClass] { topic =>
      withSubscription[SampleClass](topic.id) { subscription =>
        val attributes = SampleMap1 + (NamedIdAttribute.Default.name -> randomString())
        val messagePrototype = (SampleJson1, attributes)

        publishMessages(topic.id, Seq.fill(10)(messagePrototype): _*)

        val (messages, _) = sc.subscribeJsonFromPubsub(
          subscription = subscription,
          readConfiguration = JsonReadConfiguration().withIdAttribute(NamedIdAttribute.Default)
        )

        val messagesSink = InMemorySink(messages)

        val run = sc.run()

        eventually {
          messagesSink.toSeq.loneElement should be(PubsubMessage(SampleObject1, attributes))
        }

        run.pipelineResult.cancel()
      }
    }
  }

  it should "subscribe JSON with timestamp attribute" in withScioContext { implicit sc =>
    options.setBlockOnRun(false)

    withTopic[SampleClass] { topic =>
      withSubscription[SampleClass](topic.id) { subscription =>
        val timestamp = Instant.now()
        val attributes = SampleMap1 + (NamedTimestampAttribute.Default.name -> timestamp.toString)

        publishMessages(topic.id, (SampleJson1, attributes))

        val (messages, _) = sc.subscribeJsonFromPubsub(
          subscription = subscription,
          readConfiguration = JsonReadConfiguration().withTimestampAttribute(NamedTimestampAttribute.Default)
        )

        val messagesSink = InMemorySink(messages.withTimestamp)

        val run = sc.run()

        eventually {
          val (msg, ts) = messagesSink.toElement
          msg should be(PubsubMessage(SampleObject1, attributes))
          ts should be(timestamp)
        }

        run.pipelineResult.cancel()
      }
    }
  }
}
