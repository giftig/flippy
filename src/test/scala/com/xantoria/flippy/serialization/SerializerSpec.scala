package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.{Condition, NamespacedCondition}

class SerializerSpec extends BaseSpec {
  val engine = SerializationEngine()
  implicit val formats = DefaultFormats + engine

  "The serialization engine" should "error if the condition type is unrecognised" in {
    val data = """
      {
        "condition_type": "emerald_weapon",
        "wat": "this is bogus"
      }
    """
    assume(!engine.conditionTypes.contains("emerald_weapon"))
    an [MappingException] should be thrownBy parse(data).extract[Condition]
  }

  "Equals serialiser" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "equals",
        "value": "Ms. Cloud"
      }
    """
    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [Condition.Equals]
    extracted.appliesTo("Ms. Cloud") should be (true)
    extracted.appliesTo("Ms. Barrett") should be (false)
  }

  "And serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "and",
        "conditions": [
          {
            "condition_type": "equals",
            "value": "Tifa"
          },
          {
            "condition_type": "equals",
            "value": "Yuffie"
          }
        ]
      }
    """
    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [Condition.And]
    extracted.appliesTo("Tifa") should be (false)
    extracted.appliesTo("Yuffie") should be (false)
  }
  it should "fail if malformed" in {
    val noConditions = """{"condition_type": "and"}"""
    val weirdConditionsType = """{"condition_type": "and", "conditions": 123}"""

    a [MappingException] should be thrownBy parse(
      noConditions
    ).extract[Condition]
    a [MappingException] should be thrownBy parse(
      weirdConditionsType
    ).extract[Condition]
  }

  "Or serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "or",
        "conditions": [
          {
            "condition_type": "equals",
            "value": "Cloud"
          },
          {
            "condition_type": "equals",
            "value": "Tifa"
          }
        ]
      }
    """
    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [Condition.Or]
    extracted.appliesTo("Cloud") should be (true)
    extracted.appliesTo("Tifa") should be (true)
    extracted.appliesTo("Yuffie") should be (false)
  }

  it should "fail if malformed" in {
    val noConditions = """{"condition_type": "or", "conditions": null}"""
    val weirdConditionsType = """{"condition_type": "or", "conditions": 321}"""

    a [MappingException] should be thrownBy parse(
      noConditions
    ).extract[Condition]
    a [MappingException] should be thrownBy parse(
      weirdConditionsType
    ).extract[Condition]
  }

  "Not serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "not",
        "condition": {
          "condition_type": "equals",
          "value": "Ms. Cloud"
        }
      }
    """
    val parsed = parse(data).extract[Condition]
    parsed shouldBe a [Condition.Not]
    parsed.appliesTo("Ms. Cloud") should be (false)
    parsed.appliesTo("Yuffie") should be (true)
  }

  "Namespaced serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "namespaced",
        "attr": "name",
        "fallback": false,
        "condition": {
          "condition_type": "equals",
          "value": "Ms. Cloud"
        }
      }
    """
    val cloud = Map("name" -> "Ms. Cloud")
    val tifa = Map("name" -> "Tifa")
    val namelessOne = Map("always_silent" -> true)

    val parsed = parse(data).extract[Condition]
    parsed shouldBe a [NamespacedCondition]
    parsed.appliesTo(cloud) should be (true)
    parsed.appliesTo(tifa) should be (false)
    parsed.appliesTo(namelessOne) should be (false)
  }

  it should "respect fallback properly" in {
    val data = """
      {
        "condition_type": "namespaced",
        "attr": "name",
        "fallback": true,
        "condition": {
          "condition_type": "equals",
          "value": "Ms. Cloud"
        }
      }
    """
    val cloud = Map("name" -> "Ms. Cloud")
    val tifa = Map("name" -> "Tifa")
    val namelessOne = Map("always_silent" -> true)

    val parsed = parse(data).extract[Condition]
    parsed shouldBe a [NamespacedCondition]
    parsed.appliesTo(cloud) should be (true)
    parsed.appliesTo(tifa) should be (false)
    parsed.appliesTo(namelessOne) should be (true)
  }
}
