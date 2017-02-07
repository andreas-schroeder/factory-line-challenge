package com.as24

import java.util.concurrent.atomic.AtomicBoolean

import scala.util.Random

package object factory {

  trait Factory {
    def run(): Unit
  }

  trait FactorySource[T] {
    def pull(): Option[T]
  }

  trait FactorySink[T] {
    def put(data: T): Unit
  }

  trait QCService {
    def isOk(presentBox: PresentBox): Boolean
  }

  case class Order(items: Seq[Item])

  sealed trait Item
  case object ToyCar extends Item
  case object CellPhone extends Item
  case object Sweets extends Item
  case object Book extends Item
  case object Laptop extends Item

  case class PresentBox(seq: Seq[Item]) {
    private val isOpened: AtomicBoolean = new AtomicBoolean(false)
    override def hashCode(): Int = Random.nextInt()
  }

}

