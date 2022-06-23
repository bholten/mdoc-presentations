package example

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl._

import scala.concurrent.Future

object GraphDSLBasic {
  implicit val system: ActorSystem = ActorSystem("GraphDSLBasic")

  val input: Source[Int, NotUsed] = Source(1 to 10)
  val evens: Flow[Int, Int, NotUsed] = Flow[Int].filter(_ % 2 == 0)
  val odds: Flow[Int, Int, NotUsed] = Flow[Int].filterNot(_ % 2 == 0)
  val incrementer: Flow[Int, Int, NotUsed] = Flow[Int].map(_ + 1)
  val multiplier: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 10)
  val printer: Sink[Int, Future[Done]] = Sink.foreach[Int](println)
  val sum: Sink[Int, Future[Int]] = Sink.fold[Int, Int](0) { (acc, next) =>
    println(s"Summing: $acc + $next = ${acc + next}")
    acc + next
  }
  val subtractThenPrint: Sink[(Int, Int), Future[Done]] = Sink
    .foreach { case (a, b) =>
      println(s"Subtracting $a - $b = ${a - b}")
    }

  val graph: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val bcast = builder.add(Broadcast[Int](2))
      val iBcast = builder.add(Broadcast[Int](2))
      val mBcast = builder.add(Broadcast[Int](2))
      val zip = builder.add(Zip[Int, Int]())

      // format: OFF
      input ~> bcast.in
               bcast.out(0) ~> evens ~> incrementer ~> iBcast.in
                                                       iBcast.out(0) ~> printer
                                                       iBcast.out(1)            ~> zip.in0
               bcast.out(1) ~> odds  ~> multiplier  ~> mBcast.in
                                                       mBcast.out(0) ~> sum
                                                       mBcast.out(1)            ~> zip.in1
                                                                                   zip.out ~> subtractThenPrint
      // format: ON

      ClosedShape // MUST return a shape
    } // returns a graph
  ) // returns a runnable graph

  def main(args: Array[String]): Unit = {
    graph.run()
    ()
  }
}
