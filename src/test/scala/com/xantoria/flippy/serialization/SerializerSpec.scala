package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.{Condition, NamespacedCondition}

class SerializerSpec extends BaseSpec {
  val contextSerializer = new ContextValueSerializer()
  val engine = SerializationEngine()
  implicit val formats = DefaultFormats + contextSerializer + engine

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

  it should "serialize correctly" in {
    val c = Condition.Equals("Emerald Weapon")
    val expected = """{"condition_type":"equals","value":"Emerald Weapon"}"""
    val actual = Serialization.write(c)

    actual should be (expected)
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

  it should "serialize correctly" in {
    val c = Condition.And(List(
      Condition.Equals("Emerald Weapon"), Condition.Equals("Ruby Weapon")
    ))
    val expected = {
      """{"condition_type":"and","conditions":[""" +
      """{"condition_type":"equals","value":"Emerald Weapon"},""" +
      """{"condition_type":"equals","value":"Ruby Weapon"}]}"""
    }
    val actual = Serialization.write(c)

    actual should be (expected)
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

  it should "serialize correctly" in {
    val c = Condition.Or(List(Condition.Equals("Emerald Weapon"), Condition.Equals("Ruby Weapon")))
    val expected = {
      """{"condition_type":"or","conditions":[""" +
      """{"condition_type":"equals","value":"Emerald Weapon"},""" +
      """{"condition_type":"equals","value":"Ruby Weapon"}]}"""
    }
    val actual = Serialization.write(c)

    actual should be (expected)
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

  it should "serialize correctly" in {
    val c = Condition.Not(Condition.Equals("Midgar"))
    val expected = {
      """{"condition_type":"not","condition":""" +
      """{"condition_type":"equals","value":"Midgar"}}"""
    }
    val actual = Serialization.write(c)

    actual should be (expected)
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

  it should "serialize correctly" in {
    val c = Condition.Equals("Cloud") on "name"
    val expected = {
      """{"condition_type":"namespaced","attr":"name","condition":""" +
      """{"condition_type":"equals","value":"Cloud"},""" +
      """"fallback":false}"""
    }
    val actual = Serialization.write(c)

    actual should be (expected)
  }

  "Complex serialization conditions" should "work" in {
    // A nice complicated condition: this should match anyone named "Cloud" or who is
    // 17 years old, but only if they come from the Final Fantasy game franchise.
    val data = """
      {
        "condition_type": "and",
        "conditions": [
          {
            "condition_type": "namespaced",
            "attr": "game_franchise",
            "condition": {
              "condition_type": "equals",
              "value": "Final Fantasy"
            }
          },
          {
            "condition_type": "or",
            "conditions": [
              {
                "condition_type": "namespaced",
                "attr": "name",
                "condition": {
                  "condition_type": "equals",
                  "value": "Cloud"
                }
              },
              {
                "condition_type": "namespaced",
                "attr": "age",
                "condition": {
                  "condition_type": "equals",
                  "value": 17
                }
              }
            ]
          }
        ]
      }
    """

    val tifa = Map(
      "name" -> "Tifa",
      "age" -> 20,
      "game_franchise" -> "Final Fantasy"
    )
    val cloud = Map(
      "name" -> "Cloud",
      "age" -> 21,
      "game_franchise" -> "Final Fantasy"
    )
    val yuffie = Map(
      "name" -> "Yuffie",
      "age" -> 17,
      "game_franchise" -> "Final Fantasy"
    )
    val kid = Map(
      "name" -> "Kid",
      "age" -> 17, // She's actually 16 in the game but never mind
      "game_franchise" -> "Chrono series"
    )

    val parsed = parse(data).extract[Condition]
    parsed.appliesTo(tifa) should be (false)  // Correct franchise, wrong name, wrong age
    parsed.appliesTo(cloud) should be (true)  // Correct franchise, correct name, wrong age
    parsed.appliesTo(yuffie) should be (true) // Correct franchise, wrong name, correct age
    parsed.appliesTo(kid) should be (false)   // Wrong franchise, wrong name, correct age
  }
}
