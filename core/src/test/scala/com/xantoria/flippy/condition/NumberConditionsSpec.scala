package com.xantoria.flippy.condition

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class NumberConditionsSpec extends BaseSpec {
  import NumberConditions._

  "Number conditions" should "allow matching a range" in {
    val r = new Range(Some(2.2), Some(8.7))
    r.appliesTo(1) should be (false)
    r.appliesTo(2.2) should be (false)
    r.appliesTo(2.25) should be (true)
    r.appliesTo(5) should be (true)
    r.appliesTo(8.6) should be (true)
    r.appliesTo(8.7) should be (false)
    r.appliesTo(10) should be (false)
  }

  it should "allow matching above a value" in {
    val r = new Range(low = Some(10), high = None)
    r.appliesTo(5) should be (false)
    r.appliesTo(9.9) should be (false)
    r.appliesTo(10) should be (false)
    r.appliesTo(10.2) should be (true)
    r.appliesTo(150) should be (true)
  }

  it should "allow matching below a value" in {
    val r = new Range(low = None, high = Some(10))
    r.appliesTo(5) should be (true)
    r.appliesTo(9.9) should be (true)
    r.appliesTo(10) should be (false)
    r.appliesTo(10.2) should be (false)
    r.appliesTo(150) should be (false)
  }

  it should "allow matching multiples" in {
    val m2 = new Multiple(2)
    m2.appliesTo(0) should be (true)
    m2.appliesTo(1) should be (false)
    m2.appliesTo(2) should be (true)
    m2.appliesTo(3) should be (false)
    m2.appliesTo(4) should be (true)
    m2.appliesTo(5) should be (false)
    m2.appliesTo(6) should be (true)

    val m3 = new Multiple(3)
    m3.appliesTo(0) should be (true)
    m3.appliesTo(1) should be (false)
    m3.appliesTo(2) should be (false)
    m3.appliesTo(3) should be (true)
    m3.appliesTo(4) should be (false)
    m3.appliesTo(5) should be (false)
    m3.appliesTo(6) should be (true)
  }

  it should "not match non-integers when checking for multiples" in {
    val m2 = new Multiple(2)
    val checks = (1 to 20) map { _ * 0.1 }
    checks foreach { d: Double => m2.appliesTo(d) should be (false) }
  }
}
