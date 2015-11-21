package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.xantoria.flippy.condition.Condition

class IncorrectConditionTypeException extends RuntimeException

abstract class Backend(implicit ec: ExecutionContext) {
  def createSwitch(name: String): Future[Unit]
  def deleteSwitch(name: String): Future[Unit]

  def configureSwitch(name: String, condition: Condition[Map[String, Any]]): Future[Unit]
  def switchConfig(name: String): Future[Condition[Map[String, Any]]]

  /**
   * Looks up the switch criteria and checks if it applies, reporting any problems
   *
   * Any exceptions which happened while looking up the switch will be thrown so that the
   * application can handle problems and decide what to do about it
   */
  def isActiveSafe(switchName: String, data: Map[String, Any]): Future[Boolean] = {
    switchConfig(switchName) map {
      case c: Condition[Map[String, Any]] => c.appliesTo(data)
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
}
