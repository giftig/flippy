package com.xantoria.flippy.db

import com.whisk.docker.scalatest.DockerTestKit
import org.scalatest._
import org.scalatest.time._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.docker.{Redis => DockerRedis}

class RedisBackendSpec extends BaseSpec with DockerRedis with DockerTestKit {
  implicit val pc = PatienceConfig(Span(10, Seconds), Span(1, Second))

  "The redis backend" should "exist" ignore {
    false should be (true)  // FIXME: Implement me!
  }
}
