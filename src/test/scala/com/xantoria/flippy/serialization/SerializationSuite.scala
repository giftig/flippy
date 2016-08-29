package com.xantoria.flippy.serialization

import org.scalatest.Suites

class SerializationSuite extends Suites(
  new SerializerSpec,
  new StringSerializerSpec,
  new NetworkingSerializerSpec
)
