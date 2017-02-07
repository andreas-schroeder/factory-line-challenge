package com.as24.factory

import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

class FactoryServiceTest extends WordSpec with Matchers {

  "Factory service" when {

    "run" should {

      "consume all items" in new Context {
        val items = getRandomItems(10)
        val source = QueueFactorySource[Item](items:_*)
        val factory = buildFactory(
          itemSource = source,
          ordersSource = QueueFactorySource[Order](Order(items))
        )

        runUntil(factory, source.queue.size() == 0)
      }

      "consume all orders" in new Context {
        val items = getRandomItems(10)
        val orders = Random.shuffle(items).map(i => Order(Seq(i)))
        val source = QueueFactorySource[Order](orders:_*)

        val factory = buildFactory(
          itemSource = QueueFactorySource[Item](items:_*),
          ordersSource = source
        )

        runUntil(factory, source.queue.size() == 0)
      }

      "qc all boxes" in new Context {
        val items = getRandomItems(10)
        val orders = Random.shuffle(items).map(i => Order(Seq(i)))
        val qCService = new BoxQCService

        val factory = buildFactory(
          itemSource = QueueFactorySource[Item](items:_*),
          ordersSource = QueueFactorySource[Order](orders:_*),
          qcService = qCService
        )

        runUntil(factory, qCService.report()._1 == orders.size)
      }

      "filter good and bad boxes" in new Context {
        val items = getRandomItems(10)
        val orders = Random.shuffle(items).map(i => Order(Seq(i)))

        val goodSink = SetFactorySink[PresentBox]()
        val badSink = SetFactorySink[PresentBox]()

        val qCService = new BoxQCService

        val factory = buildFactory(
          itemSource = QueueFactorySource[Item](items:_*),
          ordersSource = QueueFactorySource[Order](orders:_*),
          qcService = qCService,
          goodBoxesSink = goodSink,
          badBoxesSink = badSink
        )

        runUntil(factory, {
          val totalCollected = goodSink.results.size() + badSink.results.size()
          val isAllProceeded = totalCollected == items.size
          val isAllBadBoxesFinded = qCService.report()._2 == badSink.results.size()

          isAllProceeded && isAllBadBoxesFinded
        })
      }

    }

  }

  trait Context {

    val availableItems = Seq(ToyCar, CellPhone, Sweets, Book, Laptop)

    def getRandomItems(amount: Int): Seq[Item] =
      (1 to amount).map(_ => Random.shuffle(availableItems).head)

    def buildFactory(
      itemSource: FactorySource[Item] = QueueFactorySource[Item](),
      ordersSource: FactorySource[Order] = QueueFactorySource[Order](),
      qcService: QCService = new BoxQCService,
      goodBoxesSink: FactorySink[PresentBox] = SetFactorySink[PresentBox](),
      badBoxesSink: FactorySink[PresentBox] = SetFactorySink[PresentBox]()
    ) = new FactoryService(itemSource, ordersSource, qcService, goodBoxesSink, badBoxesSink)

    def runUntil(factory: Factory, untilCondition: => Boolean): Unit = {
      def blockCheck(attemptLeft: Int): Unit =
        if (!untilCondition) {
          if(attemptLeft <= 0) {
            throw new RuntimeException("Hop hey, test failed!")
          } else {
            Thread.sleep(100)
            blockCheck(attemptLeft - 1)
          }
        }

      factory.run()
      blockCheck(30)
    }

  }

}
