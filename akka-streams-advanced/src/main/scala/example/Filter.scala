package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

case class Filter[T](in: Inlet[T], whenTrue: Outlet[T], whenFalse: Outlet[T])
    extends Shape {
  override val inlets: Seq[Inlet[T]] = List(in)
  override val outlets: Seq[Outlet[T]] = List(whenTrue, whenFalse)

  override def deepCopy(): Shape =
    Filter(in.carbonCopy(), whenTrue.carbonCopy(), whenFalse.carbonCopy())
}

object Filter {
  def apply[T](fn: T => Boolean): Graph[Filter[T], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val bCast = builder.add(Broadcast[T](2))
      val filter = builder.add(Flow[T].filter(fn))
      val filterNot = builder.add(Flow[T].filterNot(fn))

      bCast.out(0) ~> filter
      bCast.out(1) ~> filterNot

      Filter(bCast.in, filter.out, filterNot.out)
    }
}

object Main {
  implicit val system: ActorSystem = ActorSystem("ExampleFilter")

  def main(args: Array[String]): Unit = {
    val source = Source(1 to 100)
    val evenSink = Sink.foreach[Int](n => println(s"Even $n"))
    val oddSink = Sink.foreach[Int](n => println(s"Odd: $n"))

    val graph = RunnableGraph.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val filter = builder.add(Filter[Int]((n: Int) => n % 2 == 0))

        // format: OFF
        source ~> filter.in
                  filter.whenTrue  ~> evenSink
                  filter.whenFalse ~> oddSink
        // format: ON

        ClosedShape
      }
    )

    val _ = graph.run()
  }
}
