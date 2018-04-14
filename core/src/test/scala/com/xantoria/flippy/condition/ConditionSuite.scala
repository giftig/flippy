package com.xantoria.flippy.condition

import org.scalatest.Suites

class ConditionSuite extends Suites(
  new BasicConditionsSpec,
  new CombinatorsSpec,
  new NetworkingSpec,
  new ProportionSpec,
  new StringConditionsSpec,
  new NumberConditionsSpec
)
