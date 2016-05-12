package com.xantoria.flippy.serialization

import net.liftweb.json.{Formats, MappingException, Serializer, TypeInfo}
import net.liftweb.json.JsonAST._

/**
 * Represents a value in context which can be compared
 *
 * We'll try to deserialize raw JValues in contexts and Equals conditions into this type,
 * so depending on serialization rules configured, we'll compare values in different ways
 */
class ContextValue(val underlying: Any)

/**
 * Defines rules for parsing a JValue into a comparable type
 *
 * Essentially this means we can define complex types for comparisons by associating classes
 * with JSON structs of various formats. The default set of rules will extract simple JValues
 * into sensible corresponding scala types, String, Int, etc.
 */
class ContextValueSerializer extends Serializer[ContextValue] {
  private val Class = classOf[ContextValue]

  def deserialize(implicit formats: Formats): PartialFunction[(TypeInfo, JValue), ContextValue] = {
    case (TypeInfo(Class, _), value) => new ContextValue(
      value match {
        case v: JString => v.extract[String]
        case v: JInt => v.extract[Int]
        case v: JDouble => v.extract[Double]
        case v: JBool => v.extract[Boolean]
        case JNull | JNothing => null
        case _ => throw new MappingException("Unable to interpret type provided")
      }
    )
  }

  def serialize(implicit formats: Formats): PartialFunction[Any, JValue] = {
    case c: ContextValue => c.underlying match {
      case v: String => JString(v)
      case v: Int => JInt(v)
      case v: Double => JDouble(v)
      case v: Boolean => JBool(v)
      case None | null => JNull
      case v => JString(v.toString)
      case _ => throw new MappingException("Unable to interpret type provided")
    }
  }
}
