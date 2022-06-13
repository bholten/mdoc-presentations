package example

import akka._
import akka.actor._
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration._

object BackpressureDemo {
  implicit val system: ActorSystem = ActorSystem()

  val fastSource: Source[Int, NotUsed] =
    Source(1 to 10000)

  val flow: Flow[Int, Int, NotUsed] =
    Flow[Int].wireTap { n =>
      println(s"Flow: $n")
    }

  val slowSink: Sink[Int, Future[Done]] =
    Sink.foreach[Int] { n =>
      Thread.sleep(1000)
      println(s"Sink: $n")
    }

  def main(args: Array[String]): Unit = {
    // fastSource.to(slowSink).run()
    // fastSource.async.to(slowSink).run()
    // fastSource.async.via(flow).async.to(slowSink).run()
    val bufferedFlow =
      flow.buffer(10, overflowStrategy = OverflowStrategy.dropHead)
    // fastSource.async.via(bufferedFlow).async.to(slowSink).run()
    val throttledSource = fastSource.throttle(1, 2.second)
    throttledSource.via(bufferedFlow).to(slowSink).run()

    ()
  }
}
