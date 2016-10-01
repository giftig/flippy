package com.xantoria.flippy.db

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration._

import com.whisk.docker.scalatest.DockerTestKit
import net.liftweb.json.{DefaultFormats => _, _}
import org.scalatest._
import org.scalatest.time._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.docker.{Redis => DockerRedis}
import com.xantoria.flippy.serialization.DefaultFormats

@Ignore
class RedisBackendSpec extends BaseSpec with DockerRedis with DockerTestKit {
  private implicit val executionContext = global
  private implicit val pc = PatienceConfig(Span(10, Seconds), Span(1, Second))
  private implicit val formats = DefaultFormats

  /**
   * Convenience method for awaiting the result for a standard period in a very short call
   */
  def fin[T](f: Future[T]): T = Await.result(f, 5.seconds)

  "The redis backend" should "allow setting and retrieving switch conditions" in {
    val switchName = "ff7-switch"
    val backend = new RedisBackend("localhost", redisPort, "flippy:test:backend:setget")
    val cond = Condition.Equals("Ms. Cloud") on "name"

    fin { backend.configureSwitch(switchName, cond) }
    fin { backend.isActive(switchName, Map("name" -> "Ms. Cloud")) } should be (true)
  }

  it should "allow listing keys" in {
    val backend = new RedisBackend("localhost", redisPort, "flippy:test:backend:list")
    val switches: List[(String, Condition)] = (1 to 15).map {
      n => {
        val name = s"ff$n"
        (name, Condition.Equals(name) on "game_name")
      }
    }.toList.sortWith { _._1 < _._1 }

    // Configure all the switches in the backend and block until that's done
    fin { Future.sequence {
      switches map {
        s => {
          val (name, cond) = s
          backend.configureSwitch(name, cond)
        }
      }
    }}

    val attempts = List(
      backend.listSwitches(offset = Some(0), limit = Some(10)),
      backend.listSwitches(offset = Some(2), limit = Some(6)),
      backend.listSwitches(offset = None, limit = None),
      backend.listSwitches(offset = Some(2), limit = None)
    )
    val expected = List(
      switches.slice(0, 10),
      switches.slice(2, 8),
      switches.slice(0, switches.size),
      switches.slice(2, switches.size)
    )

    val actual = attempts map { fin(_) }
    (actual zip expected) foreach {
      t => {
        val actualSwitches = t._1.map { _._1 }
        val expectedSwitches = t._2.map { _._1 }

        actualSwitches should be (expectedSwitches)
      }
    }
  }
}
