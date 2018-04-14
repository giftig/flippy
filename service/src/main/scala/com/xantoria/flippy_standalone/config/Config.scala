package com.xantoria.flippy_standalone.config

import scopt.OptionParser

case class Config(
  interface: String = "0.0.0.0",
  port: Int = 80,
  backend: String = "redis",
  redis: Option[RedisConfig] = Some(RedisConfig())
)

case class RedisConfig(
  host: String = "localhost",
  port: Int = 6379,
  namespace: String = "flippy",
  useHash: Boolean = false
)

object Config {
  private val supportedBackends = List("redis", "mirror", "in-memory")

  val parser = new OptionParser[Config]("flippy") {
    head("flippy", "0.1.2")

    opt[String]('i', "interface").action {
      (v, c) => c.copy(interface = v)
    }.text("Interface (host) on which to serve API. Default 0.0.0.0")

    opt[Int]('p', "port").action {
      (v, c) => c.copy(port = v)
    }.text("Port on which to serve API. Default 80")

    opt[String]('b', "backend").action {
      (v, c) => v match {
        case "redis" => c.copy(backend = v)
        case _ => c.copy(backend = v, redis = None)
      }
    }.validate {
      v => if (supportedBackends.contains(v)) {
        success
      } else {
        failure(s"Backend $v is not supported")
      }
    }.text("Database backend to use. Default is redis.")

    opt[String]("backend-host").action {
      (v, c) => c.backend match {
        case "redis" => c.copy(redis = c.redis map { _.copy(host = v) })
      }
    }.text("Host where the backend runs, if applicable")

    opt[Int]("backend-port").action {
      (v, c) => c.backend match {
        case "redis" => c.copy(redis = c.redis map { _.copy(port = v) })
      }
    }.text("Port on which the backend runs, if applicable")

    opt[String]("redis-key").action {
      (v, c) => c.backend match {
        case "redis" => c.copy(redis = c.redis map { _.copy(namespace = v, useHash = true) })
      }
    }

    opt[String]("redis-prefix").action {
      (v, c) => c.backend match {
        case "redis" => c.copy(redis = c.redis map { _.copy(namespace = v, useHash = false) })
      }
    }
  }
}
