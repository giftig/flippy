package com.xantoria.flippy.condition

import org.scalatest._

import com.xantoria.flippy.BaseSpec

class CombinatorsSpec extends BaseSpec {
  "Condition.And" should "work" in {
    val (s1, s2) = ("Cloud", "Tifa")
    val cond1 = Condition.Equals(s1)
    val cond2 = Condition.Equals(s2)
    assume(s1 != s2)
    assume(cond1.appliesTo(s1))
    assume(cond2.appliesTo(s2))

    // Two ways of creating an And condition
    val andCond1 = Condition.And(List(cond1, cond2))
    val andCond2 = cond1 && cond2

    List(andCond1, andCond2) foreach {
      c: Condition => {
        c.appliesTo(s1) should be (false)
        c.appliesTo(s2) should be (false)
      }
    }

    // Now show that cond1 && cond1 and cond2 && cond2 work expectedly
    val doubleCond1 = cond1 && cond1
    doubleCond1.appliesTo(s1) should be (true)
    doubleCond1.appliesTo(s2) should be (false)

    val doubleCond2 = cond2 && cond2
    doubleCond2.appliesTo(s1) should be (false)
    doubleCond2.appliesTo(s2) should be (true)
  }

  "Condition.Or" should "work" in {
    val (s1, s2, s3) = ("Yuffie", "Red XIII", "Cid")
    val cond1 = Condition.Equals(s1)
    val cond2 = Condition.Equals(s2)
    val cond3 = Condition.Equals(s3)
    assume(s1 != s2 && s1 != s3 && s2 != s3)
    assume(cond1.appliesTo(s1))
    assume(cond2.appliesTo(s2))
    assume(cond3.appliesTo(s3))

    // Three ways of doing the same thing
    val orCond1 = Condition.Or(List(cond1, cond2))
    val orCond2 = cond1 || cond2
    val orCond3 = Condition.Or.oneOf(List(s1, s2))

    List(orCond1, orCond2, orCond3) foreach {
      c: Condition => {
        c.appliesTo(s1) should be (true)
        c.appliesTo(s2) should be (true)
        c.appliesTo(s3) should be (false)
      }
    }
  }

  "Condition.Not" should "work" in {
    val (s1, s2) = ("Aerith", "Barret")
    val cond1 = Condition.Equals(s1)
    val cond2 = Condition.Equals(s2)

    assume(s1 != s2)
    assume(cond1.appliesTo(s1))
    assume(cond2.appliesTo(s2))

    val not1a = Condition.Not(cond1)
    val not1b = !cond1
    val not2a = Condition.Not(cond2)
    val not2b = !cond2

    List(not1a, not1b) foreach {
      c: Condition => {
        c.appliesTo(s1) should be (false)
        c.appliesTo(s2) should be (true)
      }
    }

    List(not2a, not2b) foreach {
      c: Condition => {
        c.appliesTo(s1) should be (true)
        c.appliesTo(s2) should be (false)
      }
    }
  }

  "Namespacing" should "work" in {
    val data = Map(
      "Cloud" -> "Omnislash",
      "Tifa" -> "Final Heaven",
      "Barret" -> "Catastrophe"
    )

    val conds = data map { case (k, v) => (k, Condition.Equals(v)) }
    conds.foreach { case (k, v) => assume(v.appliesTo(data(k))) }

    val namespacedFalse = conds map {
      case (k: String, c: Condition) => c on k
    }
    val namespacedTrue = conds map {
      case (k: String, c: Condition) => c on (k, true)
    }

    (namespacedFalse ++ namespacedTrue) foreach {
      c: Condition => {
        c.appliesTo(data) should be (true)
      }
    }

    namespacedFalse foreach {
      c: Condition => c.appliesTo(Map()) should be (false)
    }
    namespacedTrue foreach {
      c: Condition => c.appliesTo(Map()) should be (true)
    }
  }
}
