package com.xantoria.flippy.condition

object StringConditions {
  abstract class SCondition extends Condition {
    def appliesTo(s: String): Boolean
    override def appliesTo(a: Any) = a.isInstanceOf[String] && appliesTo(a.asInstanceOf[String])
  }

  class Range(val low: Option[String], val high: Option[String]) extends SCondition {
    override def appliesTo(s: String): Boolean = {
      (low map { s > _ } getOrElse true) && (high map { s < _ } getOrElse true)
    }
  }

  class Substring(val sub: String) extends SCondition {
    override def appliesTo(s: String): Boolean = s.contains(sub)
  }
}
