package com.xantoria.flippy.serialization

import scala.reflect._

import net.liftweb.json.{Extraction, Formats, Serializer, ShortTypeHints, TypeInfo}
import net.liftweb.json.JsonAST._

import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.condition.NamespacedCondition

class MalformedConditionDefinitionException(msg: String) extends RuntimeException(msg)
class UnsupportedConditionTypeException(condType: String) extends RuntimeException(condType)

class SerializationEngine(
  val conditionTypes: List[ConditionSerializer[_]]
) extends Serializer[Condition] {
  private val Class = classOf[Condition]

  def deserialize(implicit formats: Formats): PartialFunction[(TypeInfo, JValue), Condition] = {
    case (TypeInfo(Class, _), data) => {
      val desiredType = (data \ "condition_type").extractOpt[String]
      val concrete = desiredType.map {
        desired: String => conditionTypes.find { _.typeName == desired }
      }.flatten getOrElse {
        throw new UnsupportedConditionTypeException(desiredType.getOrElse("none specified"))
      }

      concrete.deserialize(data).asInstanceOf[Condition]
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case c: Condition => {
      val serializer = conditionTypes.find { _.canSerialize(c) }.getOrElse {
        throw new UnsupportedConditionTypeException(c.getClass.getName)
      }
      serializer.serialize(c)
    }
  }
}

object SerializationEngine {
  final val DEFAULTS: List[ConditionSerializer[_]] = List(
    ConditionSerializer.And,
    ConditionSerializer.Equals,
    ConditionSerializer.Not,
    ConditionSerializer.Or,
    NamespacedConditionSerializer,
    StringConditionSerializers.Range,
    StringConditionSerializers.Substring
  )

  def apply(): SerializationEngine = new SerializationEngine(DEFAULTS)
}

abstract class ConditionSerializer[T <: Condition] {
  val typeName: String
  def typeField = JField("condition_type", JString(typeName))

  def canSerialize(condition: Condition): Boolean

  def serialize(condition: Condition)(implicit formats: Formats): JValue
  def deserialize(data: JValue)(implicit formats: Formats): T
}

object ConditionSerializer {
  object Equals extends ConditionSerializer[Condition.Equals] {
    override val typeName: String = "equals"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Equals]

    def deserialize(data: JValue)(implicit formats: Formats): Condition.Equals = {
      val value: Any = (data \ "value") match {
        case v: JString => v.extract[String]
        case v: JInt => v.extract[Int]
        case v: JDouble => v.extract[Double]
        case v: JBool => v.extract[Boolean]
        case JNull | JNothing => null
        case _ => throw new MalformedConditionDefinitionException("[Equals] Unsupported type")
      }
      new Condition.Equals(value)
    }

    override def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Equals]
      val serializedValue: JValue = cond.requiredValue match {
        case v: String => JString(v)
        case v: Int => JInt(v)
        case v: Double => JDouble(v)
        case v: Boolean => JBool(v)
        case None | null => JNull
        case v => JString(v.toString)
      }
      JObject(List(typeField, JField("value", serializedValue)))
    }
  }

  object Not extends ConditionSerializer[Condition.Not] {
    override val typeName: String = "not"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Not]

    def deserialize(data: JValue)(implicit formats: Formats): Condition.Not = Condition.Not(
      (data \ "condition").extract[Condition]
    )

    def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Not]
      JObject(List(
        typeField,
        JField("condition", Extraction.decompose(cond.inverted))
      ))
    }
  }

  object And extends ConditionSerializer[Condition.And] {
    override val typeName: String = "and"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.And]

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

    def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.And]
      JObject(List(
        typeField,
        JField("conditions", JArray(cond.subs map { Extraction.decompose(_) }))
      ))
    }
  }

  object Or extends ConditionSerializer[Condition.Or] {
    override val typeName: String = "or"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Or]

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

    def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Or]
      JObject(List(
        typeField,
        JField("conditions", JArray(cond.subs map { Extraction.decompose(_) }))
      ))
    }
  }
}

object NamespacedConditionSerializer extends ConditionSerializer[NamespacedCondition] {
  override val typeName: String = "namespaced"

  def canSerialize(c: Condition) = c.isInstanceOf[NamespacedCondition]

  def deserialize(data: JValue)(implicit formats: Formats): NamespacedCondition = {
    val attr = (data \ "attr").extractOpt[String] getOrElse {
      throw new MalformedConditionDefinitionException("[Namespace] Missing attr!")
    }
    val condition = (data \ "condition").extract[Condition]
    val fallback = (data \ "fallback").extractOpt[Boolean] getOrElse false
    new NamespacedCondition(attr, condition, fallback)
  }

  def serialize(c: Condition)(implicit formats: Formats): JValue = {
    val cond = c.asInstanceOf[NamespacedCondition]
    JObject(List(
      typeField,
      JField("attr", JString(cond.attr)),
      JField("condition", Extraction.decompose(cond.cond)),
      JField("fallback", JBool(cond.fallback))
    ))
  }
}
