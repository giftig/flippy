package com.xantoria.flippy.api

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

import net.liftweb.json._
import org.scalatest._
import spray.http._
import spray.testkit.ScalatestRouteTest

import com.xantoria.flippy.BaseSpec
import com.xantoria.flippy.condition.Condition
import com.xantoria.flippy.db.Backend
import com.xantoria.flippy.serialization.{DefaultFormats => FlippyFormats}
import com.xantoria.flippy.serialization.SerializationEngine

/**
 * A test backend which provides mocked data for ease of testing endpoints
 */
class TestBackend extends Backend {
  protected override implicit val ec = global
  val testCondition = Condition.Equals("TestyMcTestFace")
  val cond404 = "404"

  override def switchConfig(name: String): Future[Option[Condition]] = if (name == cond404) {
    Future(None)
  } else {
    Future(Some(testCondition))
  }
  override def isActiveSafe(switchName: String, data: Map[String, Any]): Future[Boolean] = Future(
    true
  )
  override def isActive(switchName: String, data: Map[String, Any]): Future[Boolean] = Future(true)

  override def createSwitch(name: String): Future[Unit] = ???
  override def deleteSwitch(name: String): Future[Unit] = ???
  override def configureSwitch(name: String, condition: Condition): Future[Unit] = ???
  override def listSwitches(
    offset: Option[Int], limit: Option[Int]
  ): Future[List[(String, Condition)]] = ???
}

class ApiSpec extends BaseSpec with ScalatestRouteTest with APIHandling {
  def actorRefFactory = system  // Connect the service API to the test ActorSystem

  protected override val backend = new TestBackend
  protected override implicit val ec = global

  override implicit def liftJsonFormats: Formats = FlippyFormats

  "The switch endpoint" should "respond to a GET with switch config" in {
    Get("/switch/someswitch/") ~> sealRoute(flippyRoutes) ~> check {
      responseAs[Condition] shouldBe backend.testCondition
    }
  }
  it should "respond to a GET on a bad switch with a 404" in {
    Get(s"/switch/${backend.cond404}/") ~> sealRoute(flippyRoutes) ~> check {
      status.intValue should be (404)
    }
  }
  it should "respond to a POST by updating the config" ignore {
    throw new RuntimeException("Not yet implemented.")
  }
  it should "respond to a DELETE by deleting the switch" ignore {
    throw new RuntimeException("Not yet implemented.")
  }

  "The is_active endpoint" should "tell us if a switch is active for the given context" in {
    // Just check that the call works, responding with a call to isActive on the backend
    Post(
      "/switch/someswitch/is_active/",
      Map("name" -> "Ms. Cloud", "predictability" -> "of course I used Ms. Cloud again")
    ) ~> sealRoute(flippyRoutes) ~> check {
      val resp = responseAs[IsActive]
      resp.result should be (true)
    }
  }
}
