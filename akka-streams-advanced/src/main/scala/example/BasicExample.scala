package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl._

import scala.concurrent.Future
import scala.util.{Failure, Success}

object BasicExample {
  implicit val system: ActorSystem = ActorSystem("BasicExample")

  val numbers: Source[Int, NotUsed] = Source(1 to 10)
  val double: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)
  val sum: Sink[Int, Future[Int]] =
    Sink.fold(0)((acc: Int, next: Int) => acc + next)

  def eventuallyResult: Future[Int] =
    numbers.via(double).runWith(sum)

  def graphDsl: RunnableGraph[Future[Int]] =
    RunnableGraph.fromGraph(
      GraphDSL.createGraph(sum) { implicit builder => sumShape =>
        {
          import GraphDSL.Implicits._
          val input = builder.add(numbers)
          val doubler = builder.add(double)

          // format: OFF
          input.out ~> doubler.in
                       doubler.out ~> sumShape.in
          // format: ON

          ClosedShape
        }
      }
    )

  def result: Future[Int] = graphDsl.run()

  def main(args: Array[String]): Unit = {
    import system.dispatcher

    result.onComplete {
      case Success(value) =>
        println(s"Value: $value")
        system.terminate()
      case Failure(exception) =>
        println(s"Error: $exception")
        system.terminate()
    }
  }
}
