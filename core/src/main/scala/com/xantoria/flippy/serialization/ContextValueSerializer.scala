package com.xantoria.flippy.serialization

import spray.json._

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
class ContextValueFormat extends JsonFormat[ContextValue] {
  def read(value: JsValue): ContextValue = new ContextValue {
    value match {
      case JsString(s: String) => s
      case JsNumber(n: BigDecimal) if n.isValidInt => n.toInt
      case JsNumber(n: BigDecimal) => n.toDouble
      case v: JsBoolean => v.value
      case v: JsArray => v.elements.map { e => read(e).underlying }.toList
      case v: JsObject => v.obj.fields mapValues { e => read(e).underlying }
      case JsNull => null
      case _ => throw new DeserializationException("Unable to interpret type provided")
    }
  }

  def write(c: ContextValue): JsValue = c.underlying match {
    case v: String => JsString(v)
    case v: Int => JsNumber(v)
    case v: Double => JsNumber(v)
    case v: Boolean => JsBoolean(v)
    case v: List[Any] => JsArray(v map { a: Any => write(new ContextValue(a)) })
    case v: Map[String, Any] => JsObject(v mapValues { a: Any => write(new ContextValue(a)) })
    case None | null => JsNull
    case v => JsString(v.toString)
  }
}
