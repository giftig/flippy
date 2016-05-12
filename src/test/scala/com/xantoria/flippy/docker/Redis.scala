package com.xantoria.flippy.docker

import com.whisk.docker._

trait Redis extends DockerKit {
  val redisPort = 26379
  val redisContainer = DockerContainer("redis:latest")
    .withPorts(6379 -> Some(redisPort))
    .withReadyChecker(
      DockerReadyChecker.LogLineContains("The server is now ready to accept connections")
    )

  abstract override def dockerContainers: List[DockerContainer] = {
    redisContainer :: super.dockerContainers
  }
}
