package com.xantoria.flippy.condition


import org.scalatest._

import com.xantoria.flippy.BaseSpec

class ProportionSpec extends BaseSpec {
  // Some sample strings producing low, middling, and high value SHA1 sums
  val lowSample = "sample-86"     // 009ba...
  val midSampleLow = "sample-100" // 7f867...
  val midSampleHigh = "sample-59" // 80183...
  val highSample = "sample-10"    // fed79...

  "A proportion condition" should "segment by SHA-1" in {
    val twentyPercent = Condition.Proportion(0.2)
    val fiftyPercent = Condition.Proportion(0.5)
    val eightyPercent = Condition.Proportion(0.8)
    val hundredPercent = Condition.Proportion(1.0)

    twentyPercent.appliesTo(lowSample) should be (true)
    fiftyPercent.appliesTo(lowSample) should be (true)
    eightyPercent.appliesTo(lowSample) should be (true)
    hundredPercent.appliesTo(lowSample) should be (true)

    twentyPercent.appliesTo(midSampleLow) should be (false)
    fiftyPercent.appliesTo(midSampleLow) should be (true)
    eightyPercent.appliesTo(midSampleLow) should be (true)
    hundredPercent.appliesTo(midSampleLow) should be (true)

    twentyPercent.appliesTo(midSampleHigh) should be (false)
    fiftyPercent.appliesTo(midSampleHigh) should be (false)
    eightyPercent.appliesTo(midSampleHigh) should be (true)
    hundredPercent.appliesTo(midSampleHigh) should be (true)

    twentyPercent.appliesTo(highSample) should be (false)
    fiftyPercent.appliesTo(highSample) should be (false)
    eightyPercent.appliesTo(highSample) should be (false)
    hundredPercent.appliesTo(highSample) should be (true)
  }
}
