package com.xantoria.flippy.condition

object StringConditions {
  abstract class SCondition extends Condition[String]

  class Range(low: Option[String], high: Option[String]) extends SCondition {
    def appliesTo(s: String): Boolean = {
      (low map { s > _ } getOrElse true) && (high map { s < _ } getOrElse true)
    }
  }

  class Substring(ss: String) extends SCondition {
    def appliesTo(s: String): Boolean = s.contains(ss)
  }
}
