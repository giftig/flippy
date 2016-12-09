package com.xantoria.flippy.api

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import akka.actor.Actor
import net.liftweb.json._
import org.slf4j.{Logger, LoggerFactory}
import spray.httpx.LiftJsonSupport
import spray.routing._

import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.db.Backend
import com.xantoria.flippy.serialization.ContextValue

/**
 * Generic info message about the state of an API request
 */
case class InfoMessage(success: Boolean = true, reason: Option[String] = None)

/**
 * Wrapper for an API "is_active" response
 */
case class IsActive(result: Boolean)

/**
 * Represents a switch definition handled by the API
 */
case class SwitchConfig(name: String, condition: Condition)

object ErrorMessage {
  def apply(t: Throwable): InfoMessage = InfoMessage(
    success = false,
    reason = Some(s"An internal error occurred: ${t.getClass.getName}: ${t.getMessage}")
  )
}

trait APIHandling extends HttpService with LiftJsonSupport {
  protected val backend: Backend
  protected implicit val ec: ExecutionContext
  private val logger = LoggerFactory.getLogger(classOf[APIHandling])

  protected implicit def exceptionHandler: ExceptionHandler = ExceptionHandler {
    case NonFatal(e) => complete {
      logger.error("Unexpected exception", e)
      500 -> ErrorMessage(e)
    }
  }

  protected implicit def rejectionHandler: RejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(msg, cause) :: _ => {
      val errorSummary: String = cause map {
        case c: MappingException => c.cause.getMessage
        case c => c.getMessage
      } getOrElse msg

      cause foreach {
        c => logger.debug("Exception while parsing request content", c)
      }

      complete(400 -> s"Bad Request: $errorSummary")
    }
    case rejections => RejectionHandler.Default(rejections)
  }

  def handleSwitch = pathPrefix("switch" / Segment) {
    switchName: String => {
      pathEndOrSingleSlash {
        post {
          entity(as[Condition]) {
            cond => onSuccess(backend.configureSwitch(switchName, cond)) {
              _ => complete {
                logger.info(s"Switch $switchName (re)configured")
                200 -> InfoMessage()
              }
            }
          }
        } ~
        get {
          onSuccess(backend.switchConfig(switchName)) {
            cond: Option[Condition] => cond map {
              c: Condition => complete(200 -> c)
            } getOrElse {
              complete {
                logger.warn(s"Attempt to check config for non-existent switch $switchName")
                404 -> InfoMessage(success = false, reason = Some("Switch not found"))
              }
            }
          }
        } ~
        delete {
          onSuccess(backend.deleteSwitch(switchName)) {
            _ => complete {
              logger.info(s"Switch $switchName deleted")
              200 -> InfoMessage()
            }
          }
        }
      } ~
      path("is_active" ~ Slash.?) {
        post {
          entity(as[Map[String, ContextValue]]) {
            data => onSuccess(backend.isActiveSafe(switchName, data mapValues { _.underlying })) {
              b => complete {
                logger.info(
                  s"is_active called on $switchName; ${if(b) "" else "not "}active for context"
                )
                200 -> IsActive(b)
              }
            }
          }
        }
      }
    }
  }

  def handleSwitchListing = {
    path("switch" ~ Slash.?) {
      get {
        parameters("offset".as[Option[Int]]) {
          offset => onSuccess(backend.listSwitches(offset = offset, limit = Some(10))) {
            data => complete {
              logger.info(s"Listing requested from ${offset getOrElse 0}; ${data.length} returned")
              200 -> data.map { s => SwitchConfig(s._1, s._2) }
            }
          }
        }
      }
    } ~
    path("switches" / "active" ~ Slash.?) {
      post {
        entity(as[Map[String, ContextValue]]) {
          data => onSuccess(backend.listActive(data mapValues { _.underlying })) {
            activeSwitches => complete {
              logger.info(s"Active switch list requested: ${activeSwitches.length} found")
              200 -> activeSwitches
            }
          }
        }
      }
    }
  }

  def flippyRoutes = handleSwitch ~ handleSwitchListing
}

class API(val backend: Backend)(
  implicit val formats: Formats,
  implicit val ec: ExecutionContext
) extends Actor with APIHandling {
  implicit def liftJsonFormats: Formats = formats

  val system = context.system
  def actorRefFactory = context
  def receive = runRoute(flippyRoutes)
}
