package example

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._

import scala.concurrent.Future

class FilterStage[A](fn: A => Boolean) extends GraphStage[Filter[A]] {
  val in: Inlet[A] = Inlet[A]("Filter.in")
  val whenTrue: Outlet[A] = Outlet[A]("Filter.whenTrue")
  val whenFalse: Outlet[A] = Outlet[A]("Filter.whenFalse")

  override def shape: Filter[A] =
    Filter(in, whenTrue, whenFalse)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      var pulled = false

      setHandler(
        in,
        new InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            if (fn(elem)) {
              push(whenTrue, elem)
              pulled = true
            } else {
              push(whenFalse, elem)
              pulled = false
            }
          }
        }
      )

      setHandler(
        whenTrue,
        new OutHandler {
          override def onPull(): Unit =
            if (pulled) pull(in)
        }
      )

      setHandler(
        whenFalse,
        new OutHandler {
          override def onPull(): Unit =
            if (!pulled) pull(in)
        }
      )
    }
}

object FilterStage {
  implicit val system: ActorSystem = ActorSystem("FilterStage")
  val source: Source[Int, NotUsed] = Source(1 to 10)
  val evenSink: Sink[Int, Future[Done]] =
    Sink.foreach[Int](n => println(s"Even: $n"))
  val oddSink: Sink[Int, Future[Done]] =
    Sink.foreach[Int](n => println(s"Odd: $n"))

  val graph: RunnableGraph[NotUsed] =
    RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val filter = builder.add(new FilterStage[Int](_ % 2 == 0))

        // format: OFF
        source ~> filter.in
                  filter.whenTrue  ~> evenSink
                  filter.whenFalse ~> oddSink
        // format: ON

        ClosedShape
      }
    )

  def main(args: Array[String]): Unit = {
    val _ = graph.run()
  }
}
