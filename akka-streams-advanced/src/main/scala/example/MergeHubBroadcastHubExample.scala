package example

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl._

object MergeHubBroadcastHubExample {
  implicit val system: ActorSystem = ActorSystem("MergeHubBroadcastHubExample")

  def main(args: Array[String]): Unit = pubsub()

  def mergehub(): Unit = {

    val merge: Source[Int, Sink[Int, NotUsed]] =
      MergeHub.source[Int]

    val sink: Sink[Int, NotUsed] =
      merge.to(Sink.foreach[Int](println)).run()

    Source(1 to 10).runWith(sink)
    ()
  }

  def broadcastHub(): Unit = {
    val broadcast: Sink[Int, Source[Int, NotUsed]] =
      BroadcastHub.sink[Int]

    val source: Source[Int, NotUsed] =
      Source(1 to 10).runWith(broadcast)

    source.runWith(Sink.foreach[Int](println))
    ()
  }

  def pubsub(): Unit = {
    val merge = MergeHub.source[String]
    val bcast = BroadcastHub.sink[String]

    // publisher: Sink[String, NotUsed]
    // subscriber: Source[String, NotUsed]
    val (publisher, subscriber) = merge.toMat(bcast)(Keep.both).run()

    subscriber.runWith(Sink.foreach(e => println(s"Subscriber: $e")))
    subscriber.map(_.length).runWith(Sink.foreach(n => println(s"Number: $n")))

    Source(List("Chicago", "Seattle", "Miami")).runWith(publisher)
    Source(List("A", "B", "C")).runWith(publisher)

    ()
  }
}
