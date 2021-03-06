package com.xantoria.flippy.db

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration._

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.Condition

class InMemoryBackendSpec extends BaseSpec {
  private implicit val executionContext = global

  /**
   * Convenience method for awaiting the result for a standard period in a very short call
   */
  def fin[T](f: Future[T]): T = Await.result(f, 5.seconds)

  // FIXME: This test fails intermittently :(
  "The in-memory backend" should "allow setting and retrieving switch conditions" ignore {
    val backend = new InMemoryBackend()
    val switchName = "wear_dress"
    val cond = Condition.Equals("Ms. Cloud") on "username"

    backend.configureSwitch(switchName, cond)
    fin(backend.switchConfig(switchName)) should be (cond)
  }

  // FIXME: This test fails intermittently :(
  it should "show the correct switch state for an existing switch" ignore {
    val backend = new InMemoryBackend()
    val switchName = "is_terrorist"
    val avalanche = List("Cloud", "Tifa", "Barret")
    val innocents = List("Aeris", "Red XIII")
    val cond = Condition.Or.oneOf(avalanche) on "username"

    backend.configureSwitch(switchName, cond)
    avalanche foreach {
      user: String => {
        val m = Map("username" -> user)
        assume(cond.appliesTo(m))
        withClue(s"Switch state for $user:") {
          fin(backend.isActive(switchName, m)) should be (true)
        }
      }
    }
    innocents foreach {
      user: String => {
        val m = Map("username" -> user)
        assume(!cond.appliesTo(m))
        withClue(s"Switch state for $user:") {
          fin(backend.isActive(switchName, m)) should be (false)
        }
      }
    }
  }
}
