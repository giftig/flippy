package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.redis.RedisClient
import net.liftweb.json.{Formats, parse => parseJson}
import net.liftweb.json.Serialization.{write => writeJson}

import com.xantoria.flippy.condition.Condition

class RedisBackend(
  host: String,
  port: Int,
  namespace: String
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends Backend {
  private final val ALLOWED_SWITCH_PATTERN = """^[\w-_]{1,64}$""".r

  private def client = new RedisClient(host, port)

  /**
   * Make sure the switch name provided is acceptable for this backend, or throw an exception
   */
  private def validateName(s: String): Unit = s match {
    case ALLOWED_SWITCH_PATTERN() => ()
    case _ => throw new IllegalArgumentException(s"The switch name '$s' is not acceptable")
  }

  /**
   * Use the given RedisClient to fetch switch config
   *
   * This is a convenience util to allow collecting multiple configs with one client if desired
   */
  def _switchConfig(name: String, c: RedisClient): Future[Option[Condition]] = Future {
    validateName(name)

    c.get(s"$namespace:$name") map {
      parseJson(_).extract[Condition]
    }
  }

  def deleteSwitch(name: String): Future[Unit] = Future {
    client.del(s"$namespace:$name")
  }

  def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    validateName(name)

    val data = writeJson(condition)
    client.set(s"$namespace:$name", data)
  }
  def switchConfig(name: String): Future[Option[Condition]] = _switchConfig(name, client)

  /**
   * Be sparing with this, as it's not possible to paginate the redis calls in order
   *
   * Caching pages of these switches is likely a good idea with this backend and can be
   * built on top of the backend if desired (especially with large numbers of switches)
   */
  def listSwitches(offset: Option[Int], limit: Option[Int]): Future[List[(String, Condition)]] = {
    val c = client

    def fetchKeys(cursor: Int = 0, acc: List[String] = Nil): Future[List[String]] = {
      // Yo dawg, I herd you liek Options...
      val res: Future[Option[(Option[Int], Option[List[Option[String]]])]] = Future {
        c.scan[String](cursor, s"$namespace:*")
      }

      res flatMap {
        _ map {
          response: (Option[Int], Option[List[Option[String]]]) => {
            val cur: Int = response._1 getOrElse 0
            val flatResult: List[String] = (response._2 getOrElse Nil).flatten map {
              _.drop(namespace.length + 1)
            }
            val data: List[String] = acc ++ flatResult

            if (cur != 0) {
              fetchKeys(cur, data)
            } else {
              Future(data)
            }
          }
        } getOrElse { Future(Nil) }
      }
    }

    val from = offset getOrElse 0
    val to = limit map { _ + from }
    val keys: Future[List[String]] = fetchKeys() map {
      k: List[String] => k.sorted.slice(from, to getOrElse k.length)
    }

    // TODO: Use a dedicated future pool for the Future.traverse here
    keys flatMap {
      keys: List[String] => Future.traverse(keys) {
        k: String => _switchConfig(k, client) map { conf: Option[Condition] => (k, conf.get) }
      }
    }
  }

  // TODO: Define a more efficient listActive implementation using batched MGET calls
}
