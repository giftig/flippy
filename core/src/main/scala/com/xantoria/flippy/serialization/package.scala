package com.xantoria.flippy

import net.liftweb.json.{DefaultFormats => LiftDefaults, Formats}

package object serialization {
  val DefaultFormats = LiftDefaults + SerializationEngine() + new ContextValueSerializer()
}
