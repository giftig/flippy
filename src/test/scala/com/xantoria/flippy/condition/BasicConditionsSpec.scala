package com.xantoria.flippy.condition

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class BasicConditionsSpec extends BaseSpec {
  "An equals condition" should "work with Strings" in {
    val s = "AVALANCHE"
    val stringCond = new Condition.Equals(s)

    stringCond.appliesTo(s) should be (true)
    stringCond.appliesTo(s + " SOLDIER") should be (false)
  }

  it should "work with Ints" in {
    val i = 7777
    val intCond = new Condition.Equals(i)

    intCond.appliesTo(i) should be (true)
    intCond.appliesTo(i + 1) should be (false)
  }
}
