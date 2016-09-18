package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.redis.RedisClient
import net.liftweb.json.{Formats, parse => parseJson}
import net.liftweb.json.Serialization.{write => writeJson}
import org.slf4j.LoggerFactory

import com.xantoria.flippy.condition.Condition

class RedisBackend(
  host: String,
  port: Int,
  namespace: String,
  batchRetrievalSize: Int = 50
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends Backend {
  private final val ALLOWED_SWITCH_PATTERN = """^[\w-_]{1,64}$""".r
  private val logger = LoggerFactory.getLogger(classOf[RedisBackend])

  logger.info(s"Using redis backend at $host:$port")
  private def client = new RedisClient(host, port)

  /**
   * Make sure the switch name provided is acceptable for this backend, or throw an exception
   */
  private def validateName(s: String): Unit = s match {
    case ALLOWED_SWITCH_PATTERN() => ()
    case _ => {
      logger.warn(s"Rejected switch name $s")
      throw new IllegalArgumentException(s"The switch name '$s' is not acceptable")
    }
  }

  /**
   * Use the given RedisClient to fetch switch config
   *
   * This is a convenience util to allow collecting multiple configs with one client if desired
   */
  private def _switchConfig(name: String, c: RedisClient): Future[Option[Condition]] = Future {
    logger.info(s"Getting config for switch $name")
    validateName(name)

    c.get(s"$namespace:$name") map {
      parseJson(_).extract[Condition]
    }
  }

  /**
   * Get a whole batch of switch configs with a single redis MGET call
   */
  private def switchConfigs(names: String*)(c: RedisClient): Future[
    List[Option[Condition]]
  ] = Future {
    logger.info(s"Getting config for a batch of ${names.length} switches")

    val keys = names.map { name => s"$namespace:$name" }

    keys match {
      case head :: tail => c.mget(head, tail: _*) map {
        results: List[Option[String]] => {
          results map {
            _.map {
              parseJson(_).extract[Condition]
            }
          }
        }
      } getOrElse Nil
      case _ => Nil
    }
  }


  def deleteSwitch(name: String): Future[Unit] = Future {
    logger.info(s"Deleting switch $name")
    client.del(s"$namespace:$name")
  }

  def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    logger.info(s"Setting config for switch $name")
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
    logger.info("Scanning redis for switches")
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
    val switches: Future[List[String]] = fetchKeys() map {
      k: List[String] => k.sorted.slice(from, to getOrElse k.length)
    }

    // TODO: Use a dedicated future pool for the Future.traverse here
    switches flatMap {
      keys: List[String] => Future.traverse(keys.grouped(batchRetrievalSize).toList) {
        batch: List[String] => switchConfigs(batch: _*)(client) map {
          results => (batch zip results) collect {
            // Prevents a None.get in case of race conditions, but a None shouldn't
            // generally exist for the result
            case (name, result) if result.isDefined => (name, result.get)
          }
        }
      }.map { _.flatten }
    }
  }

}
