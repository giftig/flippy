package com.xantoria.flippy.condition

import java.net.InetAddress

import com.xantoria.flippy.utils.Net

object Networking {
  class IPRange(val addr: InetAddress, val prefix: Int) extends Condition {
    override def appliesTo(value: Any): Boolean = {
      value.isInstanceOf[String] &&
      Net.addressInRange(InetAddress.getByName(value.asInstanceOf[String]), addr, prefix)
    }
  }

  object IPRange {
    def apply(s: String): IPRange = {
      val (addr, prefix) = Net.interpretRange(s)
      new IPRange(addr, prefix)
    }
  }
}
