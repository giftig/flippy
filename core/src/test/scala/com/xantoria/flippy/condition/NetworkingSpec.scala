package com.xantoria.flippy.condition

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class NetworkingSpec extends BaseSpec {
  "Networking.IPRange" should "work" in {
    val cond = Networking.IPRange("192.168.1.1/24")
    cond.appliesTo("192.168.1.64") should be (true)
    cond.appliesTo("10.100.0.64") should be (false)
  }
}
