package com.xantoria.flippy.condition

object NumberConditions {
  abstract class NCondition extends Condition {
    def appliesTo(d: Double): Boolean
    def appliesTo(i: Int): Boolean

    override def appliesTo(a: Any): Boolean = a match {
      case i: Int => appliesTo(i)
      case d: Double => appliesTo(d)
      case _ => false
    }
  }

  /**
   * Is the value within the numerical range provided?
   */
  class Range(val low: Option[Double], val high: Option[Double]) extends NCondition {
    override def appliesTo(d: Double): Boolean = {
      (low map { d > _ } getOrElse true) && (high map { d < _ } getOrElse true)
    }
    override def appliesTo(i: Int): Boolean = appliesTo(i.toDouble)
  }

  /**
   * Is the value an integer which is a multiple of n?
   */
  class Multiple(val n: Int) extends NCondition {
    def appliesTo(d: Double): Boolean = false
    def appliesTo(i: Int): Boolean = i % n == 0
  }
}
