package com.as24.factory

import java.util.concurrent.{ConcurrentHashMap, ConcurrentLinkedQueue}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import scala.util.Random
import scala.collection.JavaConversions._

class QueueFactorySource[T](val queue: java.util.concurrent.ConcurrentLinkedQueue[T]) extends FactorySource[T] {
  def pull(): Option[T] = Option(queue.poll())
}

object QueueFactorySource {
  def apply[T](t: T*) = new QueueFactorySource[T](new ConcurrentLinkedQueue[T](t))
}

class SetFactorySink[T](val results: java.util.concurrent.ConcurrentHashMap[T, Unit]) extends FactorySink[T] {
  def put(data: T): Unit = results.put(data, ())
}

object SetFactorySink {
  def apply[T](t: T*) = new SetFactorySink[T](new ConcurrentHashMap[T, Unit](t.map(_ -> ()).toMap))
}

class BoxQCService extends QCService {

  def isOk(presentBox: PresentBox): Boolean =
    withUnopenedBox(presentBox, {
      totalChecked.incrementAndGet()
      if (Random.nextInt(100) > 30) {
        true
      } else {
        brokenCount.incrementAndGet()
        false
      }
    })

  def report(): (Long, Long) = totalChecked.get() -> brokenCount.get()

  private def withUnopenedBox[T](presentBox: PresentBox, action: => T): T =
    if (isAlreadyOpened(presentBox)) {
      throw new RuntimeException("Box already opened!")
    } else {
      action
    }

  private def isAlreadyOpened(presentBox: PresentBox): Boolean = {
    val isOpenedField = classOf[PresentBox].getDeclaredField("isOpened")
    isOpenedField.setAccessible(true)

    !isOpenedField.get(presentBox).asInstanceOf[AtomicBoolean].compareAndSet(false, true)
  }

  private val totalChecked: AtomicLong = new AtomicLong(0)
  private val brokenCount: AtomicLong = new AtomicLong(0)

}
