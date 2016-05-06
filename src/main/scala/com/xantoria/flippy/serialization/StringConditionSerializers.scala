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
}
