package com.xantoria.flippy

import org.scalatest._

class MainSuite extends Suites(
  new condition.ConditionSuite,
  new db.DbSuite
)
