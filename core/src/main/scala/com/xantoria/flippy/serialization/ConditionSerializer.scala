package com.xantoria.flippy.serialization

import scala.reflect._

import spray.json._

import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.condition.NamespacedCondition

class MalformedConditionDefinitionException(msg: String) extends DeserializationException(msg)
class UnsupportedConditionTypeException(val condType: String) extends DeserializationException(
  s"Unsupported condition type: $condType"
)

class SerializationEngine(
  val conditionTypes: List[ConditionSerializer[_]]
) extends RootJsonFormat[Condition] {

  def read(v: JsValue): Condition = {
    val desiredType: JsString = v.asJsObject.getFields("condition_type").headOption collect {
      case s: JsString => s
    } getOrElse {
      throw new UnsupportedConditionTypeException(s)
    }

    val concrete = desiredType.map {
      desired: String => conditionTypes.find { _.typeName == desired }
    }.flatten getOrElse {
      throw new DeserializationException(desiredType.getOrElse("none specified"))
    }

    concrete.deserialize(data)
  }

  def write(c: Condition): JsValue = {
    val serializer = conditionTypes.find { _.canSerialize(c) }.getOrElse {
      throw new UnsupportedConditionTypeException(c.getClass.getName)
    }
    serializer.serialize(c)
  }
}

object SerializationEngine {
  final val DEFAULTS: List[ConditionSerializer[_]] = List(
    ConditionSerializer.And,
    ConditionSerializer.Equals,
    ConditionSerializer.Not,
    ConditionSerializer.Or,
    ConditionSerializer.Proportion,
    ConditionSerializer.True,
    ConditionSerializer.False,
    NamespacedConditionSerializer,
    StringConditionSerializers.Range,
    StringConditionSerializers.Regex,
    StringConditionSerializers.Substring,
    StringConditionSerializers.OneOf,
    NetworkingSerializer.IPRange,
    NumberConditionSerializers.Range,
    NumberConditionSerializers.Multiple
  )

  // TODO: why not simply a default arg on constructor?
  def apply(): SerializationEngine = new SerializationEngine(DEFAULTS)
}

abstract class ConditionSerializer[T <: Condition] {
  val typeName: String
  def typeField: (String, JsString) = ("condition_type", JsString(typeName)

  def canSerialize(condition: Condition): Boolean

  def write(condition: Condition)(implicit formats: Formats): JsValue
  def read(data: JsValue)(implicit formats: Formats): T
}

object ConditionSerializer {
  object Equals extends ConditionSerializer[Condition.Equals] {
    override val typeName: String = "equals"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Equals]

    override def read(data: JsValue)(implicit formats: Formats): Condition.Equals = {
      data.asJsObject.getFields("value") map {
        case v => Condition.Equals(v.convertTo[ContextValue].underlying)
      } getOrElse { throw new DeserializationException("Missing value") }
    }

    override def write(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Equals]
      JsObject(Map("value" -> new ContextValue(cond.requiredValue).toJson) + typeField)
    }
  }

  object Not extends ConditionSerializer[Condition.Not] {
    override val typeName: String = "not"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Not]

    override def read(data: JsValue)(implicit formats: Formats): Condition.Not = {
      data.asJsObject.getFields("condition") map {
        v => Condition.Not(v)
      } getOrElse { throw new DeserializationException("Missing condition") }
    }

    override def write(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Not]
      JsObject(Map("not" -> cond.inverted.toJson))
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

  object Proportion extends ConditionSerializer[Condition.Proportion] {
    override val typeName: String = "proportion"

    def canSerialize(c: Condition) = c.isInstanceOf[Condition.Proportion]

    def deserialize(data: JValue)(implicit formats: Formats): Condition.Proportion = {
      val prop = (data \ "proportion").extract[Double]
      Condition.Proportion(prop)
    }

    def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Condition.Proportion]
      JObject(List(
        typeField,
        JField("proportion", JDouble(cond.prop))
      ))
    }
  }

  object True extends ConditionSerializer[Condition] {
    override val typeName: String = "true"

    def canSerialize(c: Condition) = c == Condition.True

    def deserialize(data: JValue)(implicit formats: Formats): Condition = Condition.True
    def serialize(c: Condition)(implicit formats: Formats): JValue = JObject(List(typeField))
  }

  object False extends ConditionSerializer[Condition] {
    override val typeName: String = "false"

    def canSerialize(c: Condition) = c == Condition.False

    def deserialize(data: JValue)(implicit formats: Formats): Condition = Condition.False
    def serialize(c: Condition)(implicit formats: Formats): JValue = JObject(List(typeField))
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
