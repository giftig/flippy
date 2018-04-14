package com.xantoria.flippy_standalone

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import akka.actor._
import akka.io.{IO => AkkaIO}
import akka.pattern.ask
import akka.util.Timeout
import net.liftweb.json.Formats
import org.slf4j.LoggerFactory
import spray.can.Http

import com.xantoria.flippy.api.{API => FlippyAPI}
import com.xantoria.flippy.db._
import com.xantoria.flippy.serialization.DefaultFormats
import com.xantoria.flippy_standalone.config.Config

object Main {
  private val logger = LoggerFactory.getLogger("main")
  private implicit val ec = ExecutionContext.global
  private implicit val formats: Formats = DefaultFormats

  // TODO: Support more backends
  def configureBackend(cfg: Config): Backend = cfg.backend match {
    case "redis" => {
      val redis = cfg.redis.get

      if (redis.useHash) {
        new RedisHashBackend(redis.host, redis.port, redis.namespace)
      } else {
        new RedisBackend(redis.host, redis.port, redis.namespace)
      }
    }
    case "mirror" => ???
    case "in-memory" => ???
  }

  def main(args: Array[String]): Unit = {
    val cfg = Config.parser.parse(args, Config()).get
    logger.info(s"Starting flippy service on ${cfg.interface}:${cfg.port}...")

    val backend = configureBackend(cfg)

    implicit val timeout = Timeout(5.seconds)
    implicit val system = ActorSystem("flippy")

    val service = system.actorOf(Props(new FlippyAPI(backend)))
    val bindResult = AkkaIO(Http) ? Http.Bind(service, interface = cfg.interface, port = cfg.port)
    bindResult foreach {
      case failure: Http.CommandFailed => {
        logger.error("Failed to bind to the interface! Shutting down...")
        logger.error(s"Details: $failure")
        System.exit(1)
      }
      case _ => ()
    }
  }
}
