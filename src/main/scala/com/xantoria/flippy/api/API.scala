package com.xantoria.flippy.api

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}
import scala.util.control.NonFatal

import akka.actor.Actor
import net.liftweb.json._
import spray.httpx.LiftJsonSupport
import spray.routing.{ExceptionHandler, HttpService}

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

  protected implicit def exceptionHandler = ExceptionHandler {
    case NonFatal(e) => complete(500 -> ErrorMessage(e))
  }

  def handleSwitch = pathPrefix("switch" / Segment) {
    switchName: String => {
      pathEndOrSingleSlash {
        post {
          entity(as[Condition]) {
            cond => onSuccess(backend.configureSwitch(switchName, cond)) {
              _ => complete(200 -> InfoMessage())
            }
          }
        } ~
        get {
          onSuccess(backend.switchConfig(switchName)) {
            cond: Option[Condition] => cond map {
              c: Condition => complete(200 -> c)
            } getOrElse {
              complete(404 -> InfoMessage(success = false, reason = Some("Switch not found")))
            }
          }
        } ~
        put {
          complete(400 -> InfoMessage(success = false, reason = Some("Not implemented!")))
        } ~
        delete {
          onSuccess(backend.deleteSwitch(switchName)) {
            _ => complete(200 -> InfoMessage(success = true))
          }
        }
      } ~
      path("is_active" ~ Slash.?) {
        post {
          entity(as[Map[String, ContextValue]]) {
            data => onSuccess(backend.isActiveSafe(switchName, data mapValues { _.underlying })) {
              b => complete(200 -> IsActive(b))
            }
          }
        }
      }
    }
  }

  def handleSwitchListing = path("switch" ~ Slash.?) {
    get {
      parameters("offset".as[Option[Int]]) {
        offset => onSuccess(backend.listSwitches(offset = offset, limit = Some(10))) {
          data => complete(
            200 -> data.map { s => SwitchConfig(s._1, s._2) }
          )
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
