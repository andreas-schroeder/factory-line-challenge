package com.as24.factory

import akka.NotUsed
import akka.actor.{Actor, ActorRef, ActorSystem, Cancellable, Props}
import akka.stream._
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.GraphDSL.Builder
import akka.stream.scaladsl._

import scala.concurrent.duration._

class FactoryService (
  itemSource: FactorySource[Item],
  ordersSource: FactorySource[Order],
  qcService: QCService,
  goodBoxesSink: FactorySink[PresentBox],
  badBoxesSink: FactorySink[PresentBox]
) extends Factory {

  val maxParallelBoxing = 5

  def run(): Unit = {
    implicit val stock = sendItemsToStock()
    val boxes = boxOrders
    dispatchBoxes(boxes)
  }

  implicit val system = ActorSystem("factory")
  implicit val materializer = ActorMaterializer()

  def boxOrders(implicit stock: ActorRef): Source[PresentBox, NotUsed] = {

    def boxOrder(order: Order): Source[PresentBox, NotUsed] =
      from(new OrderBoxingActor(order.items, stock))

    from(ordersSource).flatMapMerge(maxParallelBoxing, boxOrder)
  }

  def sendItemsToStock(): ActorRef = {
    val stock = system.actorOf(Props(new StockActor()))
    from(itemSource).runWith(Sink.actorRef(stock, OutOfBusiness))
    stock
  }

  def dispatchBoxes(boxes: Source[PresentBox, NotUsed]): Unit = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val partition = builder.add(Partition[PresentBox](2, box => if (qcService.isOk(box)) 0 else 1))

      boxes ~> partition ~> to(goodBoxesSink)
               partition ~> to(badBoxesSink)
      ClosedShape
    }).run()
  }

  def from[T](actor: => ActorPublisher[T]): Source[T, NotUsed] =
    Source.fromPublisher(ActorPublisher[T](system.actorOf(Props(actor))))

  def from[T](source: FactorySource[T]) = Source.unfold(source) { s => s.pull().map(i => (s, i)) }

  def to[T](sink: FactorySink[T]) = Sink.foreach(sink.put)
}

case object OutOfBusiness

case class GetItems(items: Seq[Item])

case class ItemsFromStock(items: Seq[Item])

case object CheckStock

class StockActor(var stock: Seq[Item] = Nil) extends Actor {

  override def receive = {
    case i: Item => stock = i +: stock

    case GetItems(order) =>
      val fetchedItems = order.filter(stock.contains)
      stock = stock diff fetchedItems
      sender ! ItemsFromStock(fetchedItems)

    case OutOfBusiness => ()
  }
}

class OrderBoxingActor(order: Seq[Item], stockActor: ActorRef) extends ActorPublisher[PresentBox] {

  var itemsToFetch = order.to[Vector]

  var checkRepeatedly: Cancellable = _

  override def preStart() = {
    implicit val ec = context.dispatcher
    checkRepeatedly = context.system.scheduler.schedule(50.millis, 50.millis, self, CheckStock)
  }

  override def postStop() = checkRepeatedly.cancel()

  override def receive = {
    case CheckStock => stockActor ! GetItems(itemsToFetch)

    case ItemsFromStock(items) =>
      itemsToFetch = itemsToFetch diff items
      if(itemsToFetch.isEmpty) {
        onNext(PresentBox(order))
        onComplete()
        context.stop(self)
      }
  }
}
