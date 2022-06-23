package example

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration._

object KillswitchExample {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("KillSwitchExample")
    import system.dispatcher

    val numbers = Source(LazyList.from(1))
    val killSwitch = KillSwitches.single[Int]
    val sink = Sink.foreach[Int](println)

    val switch: UniqueKillSwitch =
      numbers
        .viaMat(killSwitch)(Keep.right)
        .to(sink)
        .run()

    system.scheduler.scheduleOnce(3.seconds) {
      switch.shutdown()
    }

    ()
  }
}
