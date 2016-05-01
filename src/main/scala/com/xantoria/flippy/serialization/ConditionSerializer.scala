package com.xantoria.flippy.serialization

import scala.reflect._

import net.liftweb.json.{Formats, Serializer, ShortTypeHints, TypeInfo}
import net.liftweb.json.JsonAST._

import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.condition.NamespacedCondition

class MalformedConditionDefinitionException(msg: String) extends RuntimeException(msg)
class UnsupportedConditionTypeException(condType: String) extends RuntimeException(condType)

class SerializationEngine(
  val conditionTypes: Map[String, ConditionSerializer[_]]
) extends Serializer[Condition] {
  private val Class = classOf[Condition]

  def deserialize(implicit formats: Formats): PartialFunction[(TypeInfo, JValue), Condition] = {
    case (TypeInfo(Class, _), data) => {
      val desiredType = (data \ "condition_type").extractOpt[String]
      val concrete = desiredType.map { conditionTypes.get(_) }.flatten getOrElse {
        throw new UnsupportedConditionTypeException(desiredType.getOrElse("none specified"))
      }

      concrete.deserialize(data).asInstanceOf[Condition]
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case c: Condition => JString("FIXME")
  }
}

object SerializationEngine {
  final val DEFAULTS: Map[String, ConditionSerializer[_]] = Map(
    "and" -> ConditionSerializer.And,
    "equals" -> ConditionSerializer.Equals,
    "namespaced" -> NamespacedConditionSerializer,
    "not" -> ConditionSerializer.Not,
    "or" -> ConditionSerializer.Or
  )

  def apply(): SerializationEngine = new SerializationEngine(DEFAULTS)
}

abstract class ConditionSerializer[T <: Condition] {
  def deserialize(data: JValue)(implicit formats: Formats): T
}

object ConditionSerializer {
  object Equals extends ConditionSerializer[Condition.Equals] {
    def deserialize(data: JValue)(implicit formats: Formats): Condition.Equals = {
      val value: Any = (data \ "value") match {
        case v: JString => v.extract[String]
        case v: JDouble => v.extract[Double]
        case v: JBool => v.extract[Boolean]
        case JNull | JNothing => null
        case _ => throw new MalformedConditionDefinitionException("[Equals] Unsupported type")
      }
      new Condition.Equals(value)
    }
  }

  object Not extends ConditionSerializer[Condition.Not] {
    def deserialize(data: JValue)(implicit formats: Formats): Condition.Not = Condition.Not(
      (data \ "condition").extract[Condition]
    )
  }

  object And extends ConditionSerializer[Condition.And] {
    def deserialize(data: JValue)(implicit formats: Formats): Condition.And = {
      val conditions = (data \ "conditions").extractOpt[List[JValue]] map {
        _.map { _.extract[Condition] }
      } getOrElse {
        throw new MalformedConditionDefinitionException("[And] Missing 'conditions' attribute")
      }
      if (conditions.isEmpty) {
        throw new MalformedConditionDefinitionException("[And] No conditions provided")
      }
      Condition.And(conditions)
    }
  }

  object Or extends ConditionSerializer[Condition.Or] {
    def deserialize(data: JValue)(implicit formats: Formats): Condition.Or = {
      val conditions = (data \ "conditions").extractOpt[List[JValue]] map {
        _.map { _.extract[Condition] }
      } getOrElse {
        throw new MalformedConditionDefinitionException("[Or] Missing 'conditions' attribute")
      }
      if (conditions.isEmpty) {
        throw new MalformedConditionDefinitionException("[Or] No conditions provided")
      }
      Condition.Or(conditions)
    }
  }
}

object NamespacedConditionSerializer extends ConditionSerializer[NamespacedCondition] {
  def deserialize(data: JValue)(implicit formats: Formats): NamespacedCondition = {
    val attr = (data \ "attr").extractOpt[String] getOrElse {
      throw new MalformedConditionDefinitionException("[Namespace] Missing attr!")
    }
    val condition = (data \ "condition").extract[Condition]
    val fallback = (data \ "fallback").extractOpt[Boolean] getOrElse false
    new NamespacedCondition(attr, condition, fallback)
  }
}
