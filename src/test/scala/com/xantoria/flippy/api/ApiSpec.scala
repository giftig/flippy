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

  // These are used to show different behaviours without the overhead of full-on mocking
  val cond404 = "404"
  val condBadPost = "BADPOST"
  val condBadDelete = "BADDELETE"

  override def switchConfig(name: String): Future[Option[Condition]] = if (name == cond404) {
    Future(None)
  } else {
    Future(Some(testCondition))
  }
  override def isActiveSafe(switchName: String, data: Map[String, Any]): Future[Boolean] = Future(
    true
  )
  override def isActive(switchName: String, data: Map[String, Any]): Future[Boolean] = Future(true)

  override def deleteSwitch(name: String): Future[Unit] = Future {
    if (name == condBadDelete) {
      throw new IllegalArgumentException("Bad DELETE test case")
    } else {
      ()
    }
  }
  override def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    if (name == condBadPost) {
      throw new IllegalArgumentException("Bad POST test case")
    } else {
      ()
    }
  }
  override def listSwitches(
    offset: Option[Int], limit: Option[Int]
  ): Future[List[(String, Condition)]] = ???

  override def listActive(data: Map[String, Any]): Future[List[String]] = Future(Nil)
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
  it should "respond to a POST with a success" in {
    Post("/switch/someswitch/", backend.testCondition) ~> sealRoute(flippyRoutes) ~> check {
      responseAs[InfoMessage].success should be (true)
      status.intValue should be (200)
    }
  }
  it should "respond to a DELETE with a success" in {
    Delete("/switch/someswitch/") ~> sealRoute(flippyRoutes) ~> check {
      responseAs[InfoMessage].success should be (true)
      status.intValue should be (200)
    }
  }

  // Mocking would be better, but these tests prove that the correct backend methods are hit
  // by showing that they will present a 500 if the test backend gives them an exception
  it should "hit configureSwitch on a POST" in {
    Post(
      s"/switch/${backend.condBadPost}/", backend.testCondition
    ) ~> sealRoute(flippyRoutes) ~> check {
      responseAs[InfoMessage].success should be (false)
      status.intValue should be (500)
    }
  }
  it should "hit deleteSwitch on a DELETE" in {
    Delete(s"/switch/${backend.condBadDelete}/") ~> sealRoute(flippyRoutes) ~> check {
      responseAs[InfoMessage].success should be (false)
      status.intValue should be (500)
    }
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

  "The list active endpoint" should "tell us the list of active switches for a context" in {
    Post(
      "/switches/active/",
      Map("name" -> "Tifa")
    ) ~> sealRoute(flippyRoutes) ~> check {
      val resp = responseAs[List[String]]
      resp should be (Nil)
    }
  }
}
