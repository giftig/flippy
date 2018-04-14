package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.{Condition, NumberConditions}

class NumberSerializerSpec extends BaseSpec {
  val contextSerializer = new ContextValueSerializer()
  val engine = SerializationEngine()
  implicit val formats = DefaultFormats + contextSerializer + engine

  "Number range serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "number:range",
        "low": 1.2,
        "high": 5
      }
    """

    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [NumberConditions.Range]
    val c = extracted.asInstanceOf[NumberConditions.Range]
    c.low should be (Some(1.2))
    c.high should be (Some(5.0))
  }

  it should "serialize correctly" in {
    val c = new NumberConditions.Range(low = Some(4.4), high = Some(7.0))
    val expected = """{"condition_type":"number:range","low":4.4,"high":7.0}"""
    val actual = Serialization.write(c)

    actual should be (expected)
  }

  "Multiple serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "integer:is_multiple",
        "modulo": 7
      }
    """
    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [NumberConditions.Multiple]
    val c = extracted.asInstanceOf[NumberConditions.Multiple]
    c.n should be (7)
  }

  it should "serialize correctly" in {
    val c = new NumberConditions.Multiple(44)
    val expected = """{"condition_type":"integer:is_multiple","modulo":44}"""
    val actual = Serialization.write(c)
    actual should be (expected)
  }
}
