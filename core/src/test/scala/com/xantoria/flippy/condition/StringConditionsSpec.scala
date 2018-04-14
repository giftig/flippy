package com.xantoria.flippy.condition

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class StringConditionsSpec extends BaseSpec {
  import StringConditions._

  "String conditions" should "allow matching a range" in {
    val r = new Range(Some("foxtrot"), Some("yankee"))
    r.appliesTo("bravo") should be (false)
    r.appliesTo("tango") should be (true)
    r.appliesTo("whisky") should be (true)
    r.appliesTo("zulu") should be (false)
  }

  it should "allow matching above a value" in {
    val r = new Range(low = Some("india"), high = None)
    r.appliesTo("charlie") should be (false)
    r.appliesTo("romeo") should be (true)
    r.appliesTo("zulu") should be (true)
  }

  it should "allow matching below a value" in {
    val r = new Range(low = None, high = Some("india"))
    r.appliesTo("charlie") should be (true)
    r.appliesTo("romeo") should be (false)
    r.appliesTo("zulu") should be (false)
  }

  it should "allow matching a substring" in {
    val cond1 = new Substring("mon")
    val cond2 = new Substring("key")

    cond1.appliesTo("monkey") should be (true)
    cond2.appliesTo("monkey") should be (true)
    cond1.appliesTo("keychain") should be (false)
    cond2.appliesTo("keychain") should be (true)
    cond1.appliesTo("monday") should be (true)
    cond2.appliesTo("monday") should be (false)
  }

  it should "allow matching a regex" in {
    val nocaps = new Regex("^[a-z ]+$".r)
    val caps = new Regex("^[A-Za-z ]+$".r)

    nocaps.appliesTo("regex is fun") should be (true)
    caps.appliesTo("regex is fun") should be (true)
    nocaps.appliesTo("ReGeX is Fun") should be (false)
    caps.appliesTo("ReGeX is Fun") should be (true)
  }

  it should "allow matching one of a list of values" in {
    val options = List("Cloud", "Tifa", "Red XVIII")
    val cond = new OneOf(options)

    options foreach {
      cond.appliesTo(_) should be (true)
    }
    cond.appliesTo("Sephiroth") should be (false)
  }

  it should "be false if given a non-string value" in {
    val cond = new Range(low = None, high = None)
    cond.appliesTo(12) should be (false)
  }

  it should "work when called as a generic Condition" in {
    val rangeCond: Condition = new Range(low = None, high = None)
    rangeCond.appliesTo("alpha") should be (true)
    rangeCond.appliesTo("zulu") should be (true)

    val subCond: Condition = new Substring("a")
    subCond.appliesTo("alpha") should be (true)
    subCond.appliesTo("tango") should be (true)
  }
}
