package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.collection.mutable.{Map => MMap}

import com.xantoria.flippy.condition.Condition

class InMemoryBackend(implicit val ec: ExecutionContext) extends Backend {
  private val switches = MMap[String, Condition]()

  def createSwitch(name: String): Future[Unit] = Future(())
  def deleteSwitch(name: String): Future[Unit] = Future {
    switches.remove(name)
    ()
  }

  def configureSwitch(name: String, condition: Condition): Future[Unit] = {
    Future {
      switches.put(name, condition)
      ()
    }
  }
  def switchConfig(name: String): Future[Condition] = Future(switches(name))

  def listSwitches(
    offset: Option[Int], limit: Option[Int]
  ): Future[List[(String, Condition)]] = Future {
    val from = offset getOrElse 0
    val to = limit map { _ + from } getOrElse switches.size
    switches.toList.slice(from, to)
  }
}
