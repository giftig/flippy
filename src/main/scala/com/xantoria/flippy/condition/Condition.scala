package com.xantoria.flippy.condition

import java.security.MessageDigest

abstract class Condition {
  def appliesTo(value: Any): Boolean

  def &&(c: Condition): Condition = Condition.And(List(this, c))
  def ||(c: Condition): Condition = Condition.Or(List(this, c))
  def on(attr: String, fallback: Boolean = false) = new NamespacedCondition(attr, this, fallback)
  def unary_! = Condition.Not(this)
}

object Condition {
  case class And(val subs: List[Condition]) extends Condition {
    override def appliesTo(value: Any): Boolean = subs forall { _.appliesTo(value) }
  }

  case class Or(subs: List[Condition]) extends Condition {
    override def appliesTo(value: Any): Boolean = subs exists { _.appliesTo(value) }
  }

  case class Equals(requiredValue: Any) extends Condition {
    override def appliesTo(value: Any): Boolean = value == requiredValue
  }

  case class Not(inverted: Condition) extends Condition {
    override def appliesTo(value: Any): Boolean = !inverted.appliesTo(value)
  }

  case class Proportion(prop: Double) extends Condition {
    override def appliesTo(value: Any): Boolean = {
      val hashed = MessageDigest.getInstance("sha-1").digest(value.toString.getBytes).map {
        c => f"$c%02x"
      }.mkString

      val max = BigDecimal(BigInt("f" * 20, 16))
      val thresh = max * prop
      val actual = BigDecimal(BigInt(hashed, 16))
      actual <= thresh
    }
  }

  case object True extends Condition {
    override def appliesTo(value: Any) = true
  }
  case object False extends Condition {
    override def appliesTo(value: Any) = false
  }

  object Or {
    def oneOf(requiredIn: List[Any]): Or = Or(requiredIn map { Condition.Equals(_) })
  }
}

class NamespacedCondition(
  val attr: String, val cond: Condition, val fallback: Boolean
) extends Condition {
  override def appliesTo(value: Any) = value match {
    case m: Map[_, _] =>
      m.asInstanceOf[Map[String, Any]].get(attr) map { cond.appliesTo(_) } getOrElse fallback
    case _ => fallback
  }
}
