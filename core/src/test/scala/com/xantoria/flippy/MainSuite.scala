package com.xantoria.flippy

import org.scalatest._

class MainSuite extends Suites(
  new api.ApiSuite,
  new condition.ConditionSuite,
  new db.DbSuite,
  new serialization.SerializationSuite,
  new utils.UtilsSuite
)
