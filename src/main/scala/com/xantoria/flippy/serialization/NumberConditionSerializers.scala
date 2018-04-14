package com.xantoria.flippy.serialization

import net.liftweb.json.{Extraction, Formats}
import net.liftweb.json.JsonAST._

import com.xantoria.flippy.condition.{Condition, NumberConditions}

object NumberConditionSerializers {
  // TODO: Add an inclusivity flag
  object Range extends ConditionSerializer[NumberConditions.Range] {
    override val typeName: String = "number:range"

    def canSerialize(c: Condition): Boolean = c.isInstanceOf[NumberConditions.Range]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[NumberConditions.Range]
      JObject(
        typeField :: Nil ++
        cond.low.map { d: Double => JField("low", JDouble(d)) } ++
        cond.high.map { d: Double => JField("high", JDouble(d)) }
      )
    }

    def deserialize(data: JValue)(implicit formats: Formats): NumberConditions.Range = {
      val low = (data \ "low").extractOpt[Double]
      val high = (data \ "high").extractOpt[Double]
      new NumberConditions.Range(low, high)
    }
  }

  object Multiple extends ConditionSerializer[NumberConditions.Multiple] {
    override val typeName: String = "integer:is_multiple"

    def canSerialize(c: Condition): Boolean = c.isInstanceOf[NumberConditions.Multiple]

    def serialize(condition: Condition)(implicit formats: Formats): JValue = {
      val cond = condition.asInstanceOf[NumberConditions.Multiple]
      JObject(List(
        typeField,
        JField("modulo", JInt(cond.n))
      ))
    }

    def deserialize(data: JValue)(implicit formats: Formats): NumberConditions.Multiple = {
      val n = (data \ "modulo").extractOpt[Int] getOrElse {
        throw new MalformedConditionDefinitionException(s"[$typeName] No modulo!")
      }
      new NumberConditions.Multiple(n)
    }
  }
}
