package com.xantoria.flippy.serialization

import net.liftweb.json.{Extraction, Formats}
import net.liftweb.json.JsonAST._

import com.xantoria.flippy.condition.{Condition, StringConditions}

/**
 * Serializers for `com.xantoria.flippy.condition.StringConditions classes
 */
object StringConditionSerializers {
  // TODO: Add an inclusivity flag
  object Range extends ConditionSerializer[StringConditions.Range] {
    override val typeName: String = "string:range"

    def canSerialize(c: Condition) = c.isInstanceOf[StringConditions.Range]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[StringConditions.Range]
      JObject(
        typeField :: Nil ++
        cond.low.map { s: String => JField("low", JString(s)) } ++
        cond.high.map { s: String => JField("high", JString(s)) }
      )
    }

    def deserialize(data: JValue)(implicit formats: Formats): StringConditions.Range = {
      val low = (data \ "low").extractOpt[String]
      val high = (data \ "high").extractOpt[String]
      new StringConditions.Range(low, high)
    }
  }

  object Regex extends ConditionSerializer[StringConditions.Regex] {
    override val typeName: String = "string:regex"

    def canSerialize(c: Condition) = c.isInstanceOf[StringConditions.Regex]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[StringConditions.Regex]
      JObject(List(
        typeField,
        JField("pattern", JString(cond.pattern.toString))
      ))
    }

    def deserialize(data: JValue)(implicit formats: Formats): StringConditions.Regex = {
      val pattern = (data \ "pattern").extractOpt[String] map { _.r } getOrElse {
        throw new MalformedConditionDefinitionException("[String:Regex] No pattern!")
      }
      new StringConditions.Regex(pattern)
    }
  }

  // TODO: Add a case-sensitivity flag
  object Substring extends ConditionSerializer[StringConditions.Substring] {
    override val typeName: String = "string:substring"

    def canSerialize(c: Condition) = c.isInstanceOf[StringConditions.Substring]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[StringConditions.Substring]
      JObject(List(
        typeField,
        JField("value", JString(cond.sub))
      ))
    }

    def deserialize(data: JValue)(implicit formats: Formats): StringConditions.Substring = {
      val value = (data \ "value").extractOpt[String] getOrElse {
        throw new MalformedConditionDefinitionException("[String:Substring] No value!")
      }
      new StringConditions.Substring(value)
    }
  }

  object OneOf extends ConditionSerializer[StringConditions.OneOf] {
    override val typeName: String = "string:oneof"

    def canSerialize(c: Condition) = c.isInstanceOf[StringConditions.OneOf]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[StringConditions.OneOf]
      JObject(List(
        typeField,
        JField("options", JArray(cond.options map JString.apply))
      ))
    }

    def deserialize(data: JValue)(implicit formats: Formats): StringConditions.OneOf = {
      val options = (data \ "options").extractOpt[List[JValue]] map {
        _.map { _.extract[String] }
      } getOrElse {
        throw new MalformedConditionDefinitionException(
          "[String:OneOf] Missing 'options' attribute"
        )
      }

      new StringConditions.OneOf(options)
    }
  }
}
