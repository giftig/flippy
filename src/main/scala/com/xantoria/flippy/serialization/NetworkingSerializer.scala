package com.xantoria.flippy.serialization

import java.net.InetAddress

import net.liftweb.json.{Extraction, Formats}
import net.liftweb.json.JsonAST._

import com.xantoria.flippy.condition.{Condition, Networking}

object NetworkingSerializer {
  val rangePattern = """^((?:\d{1,3}\.){3}\d{1,3})/(\d{1,2})""".r
  val addrBytesPattern = """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$""".r

  object IPRange extends ConditionSerializer[Networking.IPRange] {
    override val typeName: String = "networking:iprange"

    def canSerialize(c: Condition) = c.isInstanceOf[Networking.IPRange]

    def serialize(c: Condition)(implicit formats: Formats): JValue = {
      val cond = c.asInstanceOf[Networking.IPRange]
      JObject(List(
        typeField,
        JField("range", JString("${cond.addr.getHostAddress}/${cond.prefix}"))
      ))
    }

    def deserialize(data: JValue)(implicit formats: Formats): Networking.IPRange = {
      val range = (data \ "range").extract[String]
      val (addr: String, prefix: Int) = range match {
        case rangePattern(addr, prefix) => {
          // Check the address actually makes sense
          addr match {
            case addrBytesPattern(b1, b2, b3, b4) => {
              if (List(b1, b2, b3, b4) exists { _.toInt > 255 }) {
                throw new MalformedConditionDefinitionException(
                  "Address byte exceeded 255. Bad IPv4 address."
                )
              }
            }
          }

          // Now check the prefix isn't over 32, which would be silly
          val intPrefix = prefix.toInt
          if (intPrefix > 32) {
            throw new MalformedConditionDefinitionException(
              s"Bad prefix value /$intPrefix. IPv4 addresses are only 32 bytes long!"
            )
          }

          (addr, intPrefix)
        }
        case _ => throw new MalformedConditionDefinitionException("Bad range")
      }

      new Networking.IPRange(InetAddress.getByName(addr), prefix)
    }
  }
}
