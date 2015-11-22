package com.xantoria.flippy.serialization

import com.xantoria.flippy.condition.Condition

import net.liftweb.json.{Formats, Serializer, ShortTypeHints, TypeInfo}
import net.liftweb.json.JsonAST._

class UnsupportedConditionTypeException(condType: String) extends RuntimeException(condType)

class ConditionSerializer(
  val conditionTypes: Map[String, Class[_]]
) extends Serializer[Condition] {
  private val Class = classOf[Condition]

  private def doExtract(data: JValue, condType: Class[_])(implicit formats: Formats): Condition = {
    data.extract[Condition]
  }

  def deserialize(implicit formats: Formats): PartialFunction[(TypeInfo, JValue), Condition] = {
    case (TypeInfo(Class, _), data) => {
      val desiredType = (data \ "condition_type").extractOpt[String]
      val condType = desiredType.map { conditionTypes.get(_) }.flatten getOrElse {
        throw new UnsupportedConditionTypeException(desiredType.getOrElse("none specified"))
      }
      doExtract(data, condType)(formats + ShortTypeHints(condType :: Nil))
    }
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case c: Condition => JString("FIXME")
  }
}
