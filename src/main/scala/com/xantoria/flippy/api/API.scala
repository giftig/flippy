package com.xantoria.flippy.api

import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}

import akka.actor.Actor
import net.liftweb.json._
import spray.httpx.LiftJsonSupport
import spray.routing.HttpService

import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.db.Backend
import com.xantoria.flippy.serialization.ContextValue

/**
 * Represents a switch definition handled by the API
 */
case class InfoMessage(success: Boolean = true, reason: Option[String] = None)
case class IsActive(result: Boolean)

object ErrorMessage {
  def apply(t: Throwable): InfoMessage = InfoMessage(
    success = false,
    reason = Some("An internal error occurred: ${t.getClass.getName}: ${t.getMessage}")
  )
}

trait APIHandling extends HttpService with LiftJsonSupport {
  protected val backend: Backend
  protected implicit val ec: ExecutionContext

  def handleSwitch = pathPrefix("switch" / Segment) {
    switchName: String => {
      pathEndOrSingleSlash {
        post {
          entity(as[Condition]) {
            cond => onComplete(backend.configureSwitch(switchName, cond)) {
              case Success(_) => complete(200 -> InfoMessage())
              case Failure(e) => complete(500 -> s"An internal error occurred: ${e.getMessage}")
            }
          }
        } ~
        get {
          onComplete(backend.switchConfig(switchName)) {
            case Success(cond) => complete(200 -> cond)
            case Failure(e) => complete(500 -> ErrorMessage(e))
          }
        } ~
        put {
          complete(400 -> InfoMessage(success = false, reason = Some("Not implemented!")))
        } ~
        delete {
          complete(400 -> InfoMessage(success = false, reason = Some("Not implemented!")))
        }
      } ~
      path("is_active" ~ Slash.?) {
        post {
          entity(as[Map[String, ContextValue]]) {
            data => onComplete(backend.isActiveSafe(switchName, data mapValues { _.underlying })) {
              case Success(b) => complete(200 -> IsActive(b))
              case Failure(e) => complete(500 -> ErrorMessage(e))
            }
          }
        }
      }
    }
  }
}

class API(val backend: Backend)(
  implicit val formats: Formats,
  implicit val ec: ExecutionContext
) extends Actor with APIHandling {
  implicit def liftJsonFormats: Formats = formats

  val system = context.system
  def actorRefFactory = context
  def receive = runRoute(handleSwitch)
}
