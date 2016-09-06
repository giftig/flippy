package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.xantoria.flippy.condition.Condition

class IncorrectConditionTypeException extends RuntimeException

abstract class Backend {
  protected implicit val ec: ExecutionContext

  def deleteSwitch(name: String): Future[Unit]

  def configureSwitch(name: String, condition: Condition): Future[Unit]
  def switchConfig(name: String): Future[Option[Condition]]

  def listSwitches(offset: Option[Int], limit: Option[Int]): Future[List[(String, Condition)]]

  /**
   * Looks up the switch criteria and checks if it applies, reporting any problems
   *
   * Any exceptions which happened while looking up the switch will be thrown so that the
   * application can handle problems and decide what to do about it
   */
  def isActiveSafe(switchName: String, data: Map[String, Any]): Future[Boolean] = {
    switchConfig(switchName) map {
      c: Option[Condition] => c map { _.appliesTo(data) } getOrElse false
    }
  }

  /**
   * Check if the given switch is active for the data
   *
   * Squash any exceptions in checking the switch state and return false instead. If you want finer
   * control over exception handling, use isActiveSafe instead.
   */
  def isActive(switchName: String, data: Map[String, Any]): Future[Boolean] = {
    isActiveSafe(switchName, data) recover {
      case t: Throwable => false
    }
  }

  /**
   * Find a list of switches which are true for the given context
   *
   * The default implementation is to just hit listSwitches and run them over isActive, but for
   * most backends that's going to be inefficient, so this method will usually be overridden.
   */
  def listActive(data: Map[String, Any]): Future[List[String]] = listSwitches(
    offset = None, limit = None
  ) flatMap {
    switches: List[(String, Condition)] => Future.sequence {
      switches.map {
        switch: (String, Condition) => isActive(switch._1, data) map {
          case true => Some(switch._1)
          case false => None
        }
      }
    } map { _.flatten }
  }
}
