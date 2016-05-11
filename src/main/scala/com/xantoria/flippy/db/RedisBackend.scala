package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.redis.RedisClient
import net.liftweb.json.{Formats, parse => parseJson}
import net.liftweb.json.Serialization.{write => writeJson}

import com.xantoria.flippy.condition.Condition

class RedisBackend(
  host: String,
  port: Int,
  namespace: String
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends Backend {
  private def client = new RedisClient(host, port)

  def createSwitch(name: String): Future[Unit] = Future(())
  def deleteSwitch(name: String): Future[Unit] = Future {
    client.del(s"$namespace:$name")
  }

  def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    val data = writeJson(condition)
    client.set(s"$namespace:$name", data)
  }
  def switchConfig(name: String): Future[Condition] = Future {
    client.get(s"$namespace:$name") map {
      parseJson(_).extract[Condition]
    } getOrElse { Condition.False }
  }
}
