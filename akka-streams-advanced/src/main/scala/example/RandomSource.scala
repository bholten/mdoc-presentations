package example

import akka.stream._
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}

import scala.util.Random

class RandomSource extends GraphStage[SourceShape[Int]] {
  val out: Outlet[Int] = Outlet("RandomSource")
  private val random = new Random()

  // Shape of our custom stage
  override def shape: SourceShape[Int] = SourceShape(out)

  // Implement logic here
  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {
      // GraphStageLogic has several handlers

      setHandler(
        out,
        new OutHandler {
          // called with demand downstream
          override def onPull(): Unit = {
            val nextNum = random.nextInt()
            push(out, nextNum)
          }
        }
      )
    }
}
