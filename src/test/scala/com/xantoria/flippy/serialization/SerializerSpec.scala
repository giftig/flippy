package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.Condition

class SerializerSpec extends BaseSpec {
  "Condition serialisers" should "work" ignore {
    val c = new ConditionSerializer(Map(
      "eek" -> classOf[Condition.Equals]
    ))
    val data = """
      {
        "condition_type": "eek",
        "value": "Ms. Cloud"
      }
    """

    implicit val formats = DefaultFormats + c
    val extracted = parse(data).extract[Condition]
  }
}
