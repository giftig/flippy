package com.xantoria.flippy.condition

object StringConditions {
  abstract class SCondition extends Condition {
    def appliesTo(s: String): Boolean
    override def appliesTo(a: Any) = false
  }

  class Range(low: Option[String], high: Option[String]) extends SCondition {
    override def appliesTo(s: String): Boolean = {
      (low map { s > _ } getOrElse true) && (high map { s < _ } getOrElse true)
    }
  }

  class Substring(ss: String) extends SCondition {
    override def appliesTo(s: String): Boolean = s.contains(ss)
  }
}
