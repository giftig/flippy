package com.xantoria.flippy.db

import scala.concurrent.{ExecutionContext, Future}

import akka.actor.ActorSystem
import net.liftweb.json._
import spray.client.pipelining._
import spray.http._
import spray.httpx.LiftJsonSupport
import spray.httpx.unmarshalling.Unmarshaller

import com.xantoria.flippy.api.{IsActive, SwitchConfig}
import com.xantoria.flippy.condition.Condition

/**
 * Mirrors another flippy server by hitting its API to get switch states
 *
 * This backend is read only, to avoid race conditions. If using this backend, the application
 * shouldn't be serving its own API or hosting the admin interface, as it's a mirror of a master
 * copy. Any management should be performed on the authoritative flippy host instead.
 *
 * You'll need to make sure the mirror backend is configured with the same set of custom formats
 * as the authoritative server if you're using them, otherwise the mirror may fail to understand
 * or be understood for some switches. In particular, if you're using the switchConfig method
 * you'll need to ensure the set of switches is the same as the master, and if you're using the
 * isActive method you'll need to ensure any context formats are the same.
 */
class MirrorBackend(
  scheme: String = "http",
  authority: Uri.Authority = Uri.Authority(host = Uri.Host("localhost"), port = 80)
)(
  implicit actorSystem: ActorSystem,
  implicit val formats: Formats
) extends Backend with LiftJsonSupport {
  protected final val USER_AGENT = "Flippy-MirrorBackend/0.1.1 (com.xantoria.flippy)"
  protected implicit val ec: ExecutionContext = actorSystem.dispatcher
  implicit def liftJsonFormats: Formats = formats

  /**
   * Pipelining step to unmarshal a config request directly into an Option[Condition]
   *
   * Return a None in case of 404, attempt to unmarshal the response and provide the
   * switch config in case of 2xx, and throw an exception in all other cases
   */
  private val unmarshalConfig: (HttpResponse) => Option[Condition] = {
    (r: HttpResponse) => {
      r.status.intValue match {
        case n if n >= 200 && n <= 299 => Some(
          Unmarshaller.unmarshal[SwitchConfig](r.entity).right.get.condition
        )
        case 404 => None
        case n => throw new RuntimeException(s"Bad response from master node: $n")
      }
    }
  }

  protected val baseUrl = Uri(scheme, authority)

  // Pipelines for API requests
  private val basePipeline = addHeader("User-Agent", USER_AGENT) ~> sendReceive
  protected val configPipeline = basePipeline ~> unmarshalConfig
  protected val isActivePipeline = basePipeline ~> unmarshal[IsActive]
  protected val listPipeline = basePipeline ~> unmarshal[List[SwitchConfig]]

  /**
   * Unsupported; MirrorBackend is read-only
   */
  def deleteSwitch(name: String): Future[Unit] = Future {
    throw new UnsupportedOperationException("The mirror backend is read-only")
  }
  /**
   * Unsupported; MirrorBackend is read-only
   */
  def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    throw new UnsupportedOperationException("The mirror backend is read-only")
  }

  /**
   * Retrieve a switch's config from the authoritative flippy server
   */
  def switchConfig(name: String): Future[Option[Condition]] = configPipeline {
    Get(baseUrl.withPath(Uri.Path(s"/switch/$name/")))
  }

  /**
   * Retrieve a list of switches available on the authoritative flippy server
   */
  def listSwitches(offset: Option[Int], limit: Option[Int]): Future[List[(String, Condition)]] = {
    val params = {
      offset.map { "offset" -> _.toString } ++
      limit.map { "limit" -> _.toString }
    }.toMap

    listPipeline {
      Get(baseUrl.withPath(Uri.Path("/switch/")).withQuery(Uri.Query(params)))
    } map {
      _.map { cfg: SwitchConfig => (cfg.name, cfg.condition) }
    }
  }

  /**
   * Ask the authoritative flippy server if a given switch is active with the provided context
   */
  override def isActiveSafe(switchName: String, data: Map[String, Any]): Future[Boolean] = ???
}
