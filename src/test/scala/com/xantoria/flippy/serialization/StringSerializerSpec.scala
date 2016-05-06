package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.{Condition, StringConditions}

class StringSerializerSpec extends BaseSpec {
  val engine = SerializationEngine()
  implicit val formats = DefaultFormats + engine

  "String range serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "string:range",
        "low": "Cloud",
        "high": "Tifa"
      }
    """

    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [StringConditions.Range]
    extracted.appliesTo("Red XIII") should be (true)
    extracted.appliesTo("Yuffie") should be (false)
  }
  it should "serialize correctly" in {
    val c = new StringConditions.Range(low = Some("Ms. Cloud"), high = Some("Seffy"))
    val expected = """{"condition_type":"string:range","low":"Ms. Cloud","high":"Seffy"}"""
    val actual = Serialization.write(c)

    actual should be (expected)
  }

  "Substring serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "string:substring",
        "value": "weapon"
      }
    """

    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [StringConditions.Substring]
    extracted.appliesTo("Cloud") should be (false)
    extracted.appliesTo("Tifa") should be (false)
    extracted.appliesTo("emerald weapon") should be (true)
    extracted.appliesTo("ruby weapon") should be (true)
  }

  it should "serialize correctly" in {
    val c = new StringConditions.Substring("Chocobo")
    val expected = """{"condition_type":"string:substring","value":"Chocobo"}"""
    val actual = Serialization.write(c)

    actual should be (expected)
  }
}
