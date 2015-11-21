package com.xantoria.flippy.condition

abstract class Condition[T] {
  def appliesTo(value: T): Boolean

  def &&(c: Condition[T]): Condition[T] = new Condition.And[T](List(this, c))
  def ||(c: Condition[T]): Condition[T] = new Condition.Or[T](List(this, c))
  def on(attr: String, fallback: Boolean = false) = new NamespacedCondition(attr, this, fallback)
  def unary_! = new Condition.Not[T](this)
}

object Condition {
  class And[T](subs: List[Condition[T]]) extends Condition[T] {
    def appliesTo(value: T): Boolean = subs forall { _.appliesTo(value) }
  }

  class Or[T](subs: List[Condition[T]]) extends Condition[T] {
    def appliesTo(value: T): Boolean = subs exists { _.appliesTo(value) }
  }

  class Equals[T](requiredValue: T) extends Condition[T] {
    def appliesTo(value: T): Boolean = value == requiredValue
  }

  class OneOf[T](requiredIn: List[T]) extends Condition.Or[T](
    requiredIn map { new Condition.Equals(_) }
  )

  class Not[T](inverted: Condition[T]) extends Condition[T] {
    def appliesTo(value: T) = !inverted.appliesTo(value)
  }
}

class NamespacedCondition[-T](
  attr: String, cond: Condition[T], fallback: Boolean
) extends Condition[Map[String, Any]] {
  def appliesTo(value: Map[String, Any]) = {
    value.get(attr) map {
      v: Any => v.isInstanceOf[T] && cond.appliesTo(v.asInstanceOf[T])
    } getOrElse fallback
  }
}
