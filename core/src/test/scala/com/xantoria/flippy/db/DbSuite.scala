package com.xantoria.flippy.db

import org.scalatest.Suites

class DbSuite extends Suites(
  new InMemoryBackendSpec,
  new RedisBackendSpec
)
