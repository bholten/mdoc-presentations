package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, Graph, UniformFanInShape}
import akka.stream.scaladsl._

object GraphCycles {
  implicit val system: ActorSystem = ActorSystem("GraphCycles")

  val fibonacciGraph: Graph[UniformFanInShape[BigInt, BigInt], NotUsed] =
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val zip = builder.add(Zip[BigInt, BigInt]())
      val merge = builder.add(MergePreferred[(BigInt, BigInt)](1))
      val fib = builder.add(Flow[(BigInt, BigInt)].map { case (a, b) =>
        (a + b, a)
      })
      val bcast = builder.add(Broadcast[(BigInt, BigInt)](2))
      val extract = builder.add(Flow[(BigInt, BigInt)].map(_._1))

        // format: OFF
        zip.out ~> merge    ~> fib ~> bcast ~> extract
                   merge.preferred <~ bcast
        // format: ON

      UniformFanInShape(extract.out, zip.in0, zip.in1)
    }

  val fibonacci: RunnableGraph[NotUsed] = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val a = builder.add(Source.single[BigInt](1))
      val b = builder.add(Source.single[BigInt](1))
      val f = builder.add(fibonacciGraph)
      val sink = Sink.foreach[BigInt] { n =>
        Thread.sleep(500)
        println(n)
      }

      // format: OFF
      a ~> f.in(0)
      b ~> f.in(1)
           f.out ~> sink
      // format: ON

      ClosedShape
    }
  )

  def main(args: Array[String]): Unit = {
    fibonacci.run()
    ()
  }
}
