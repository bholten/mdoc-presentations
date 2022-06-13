package example

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.duration._

class TestExample
    extends TestKit(ActorSystem("TestExample"))
    with AnyWordSpecLike
    with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "An integer stream" should {
    "test like any future" in {
      val source = Source(1 to 10)
      val sink = Sink.fold(0)((acc: Int, next: Int) => acc + next)
      val sum = source.runWith(sink)

      val result = Await.result(sum, 2.seconds)
      assert(result == 55)
    }

    "integrate with test actors" in {
      import akka.pattern.pipe
      import system.dispatcher

      val source = Source(1 to 10)
      val sink = Sink.fold(0)((acc: Int, next: Int) => acc + next)

      val probe = TestProbe()
      source.runWith(sink).pipeTo(probe.ref)

      probe.expectMsg(55)
    }

    "can test within a stream" in {
      val flow = Flow[Int].map(_ * 2)
      val testSource = TestSource.probe[Int]
      val testSink = TestSink.probe[Int]

      val materialized = testSource.via(flow).toMat(testSink)(Keep.both).run()
      val (publisher, subscriber) = materialized

      publisher
        .sendNext(1)
        .sendNext(5)
        .sendNext(42)
        .sendNext(99)
        .sendComplete()

      subscriber
        .request(4)
        .expectNext(2, 10, 84, 198)
        .expectComplete()
    }
  }
}
