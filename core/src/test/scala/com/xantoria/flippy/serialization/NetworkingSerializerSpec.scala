package com.xantoria.flippy.serialization

import net.liftweb.json._
import org.scalatest._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.{Condition, Networking}

class NetworkingSerializerSpec extends BaseSpec {
  val contextSerializer = new ContextValueSerializer()
  val engine = SerializationEngine()
  implicit val formats = DefaultFormats + contextSerializer + engine

  "IPRange serializer" should "deserialize correctly" in {
    val data = """
      {
        "condition_type": "networking:iprange",
        "range": "192.168.1.1/24"
      }
    """

    val extracted = parse(data).extract[Condition]
    extracted shouldBe a [Networking.IPRange]
    extracted.appliesTo("192.168.1.64") should be (true)
    extracted.appliesTo("10.100.0.64") should be (false)
  }

  it should "complain about bad addresses" in {
    val data = """
      {
        "condition_type": "networking:iprange",
        "range": "256.0.0.0/32"
      }
    """
    a [MappingException] should be thrownBy parse(data).extract[Condition]
  }
  it should "complain about bad prefixes" in {
    val data = """
      {
        "condition_type": "networking:iprange",
        "range": "192.168.1.1/33"
      }
    """
    a [MappingException] should be thrownBy parse(data).extract[Condition]
  }

  it should "serialize correctly" in {
    val c = Networking.IPRange("192.168.1.64/32")
    val expected = """{"condition_type":"networking:iprange","range":"192.168.1.64/32"}"""
    val actual = Serialization.write(c)

    actual should be (expected)
  }
}
