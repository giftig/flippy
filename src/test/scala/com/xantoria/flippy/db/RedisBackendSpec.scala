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
    val backend = new RedisBackend("localhost", redisPort, "testtesttest")
    val cond = Condition.Equals("Ms. Cloud") on "name"
    backend.configureSwitch(switchName, cond)

    fin { backend.isActive(switchName, Map("name" -> "Ms. Cloud")) } should be (true)
  }
}
