package com.xantoria.flippy.condition

object StringConditions {
  abstract class StringCondition extends Condition[String]

  class RangeCondition(low: Option[String], high: Option[String]) extends StringCondition {
    def appliesTo(s: String): Boolean = {
      (low map { s > _ } getOrElse true) && (high map { s < _ } getOrElse true)
    }
  }

  class SubstringCondition(ss: String) extends StringCondition {
    def appliesTo(s: String): Boolean = s.contains(ss)
  }
}
