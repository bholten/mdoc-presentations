package example

import akka.actor.ActorSystem
import akka.stream.SinkShape
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ExampleSink {
  implicit val system: ActorSystem = ActorSystem("ExampleSink")

  val sum: Sink[Int, Future[Int]] =
    Sink.fold[Int, Int](0)((acc, next) => acc + next)
  val last: Sink[Int, Future[Int]] = Sink.last[Int]

  val dualSink: Sink[Int, Future[(Int, Int)]] = {
    Sink.fromGraph(
      GraphDSL.createGraph(sum, last)(combineMat = (a, b) => a.zip(b)) {
        implicit builder => (sumShape, lastShape) =>
          {
            import GraphDSL.Implicits._
            val bCast = builder.add(Broadcast[Int](2))

            bCast.out(0) ~> sumShape
            bCast.out(1) ~> lastShape
            SinkShape(bCast.in)
          }
      }
    )
  }

  def main(args: Array[String]): Unit = {
    val source = Source(1 to 10)
    val result = source.runWith(dualSink)
    import system.dispatcher

    result.onComplete {
      case Success(value) =>
        println(value)
        system.terminate()
      case Failure(exception) =>
        println(s"Failure: $exception")
        system.terminate()
    }
  }
}
