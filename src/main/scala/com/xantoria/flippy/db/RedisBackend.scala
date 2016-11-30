package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.redis.RedisClient
import net.liftweb.json.{Formats, parse => parseJson}
import net.liftweb.json.Serialization.{write => writeJson}
import org.slf4j.LoggerFactory

import com.xantoria.flippy.condition.Condition

/**
 * Defines methods for handling fetching switches and configs from redis
 *
 * In particular, it contains abstract definitions for setting, getting, scanning, and mgetting
 * keys either from the redis keyspace or from a hash, according to the concrete definition
 * imposed by the implementing backend variant. (i.e. using either GET, SET, MGET, SCAN or
 * HGET, HSET, HMGET, HSCAN
 */
trait RedisSupport {
  protected def rget(key: String)(implicit c: RedisClient): Option[String]
  protected def rset(key: String, data: String)(implicit c: RedisClient): Boolean
  protected def rdel(key: String)(implicit c: RedisClient): Option[Long]

  // Yo dawg, I herd you liek options...
  protected def rscan(cursor: Int, count: Int = 10)(implicit c: RedisClient): Option[
    (Option[Int], Option[List[Option[String]]])
  ]
  protected def rmget(key: String, keys: String*)(
    implicit c: RedisClient
  ): Option[List[Option[String]]]
}

class RedisBackend(
  host: String,
  port: Int,
  namespace: String,
  batchRetrievalSize: Int = 50
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends Backend with RedisSupport {
  private final val ALLOWED_SWITCH_PATTERN = """^[\w-_]{1,64}$""".r
  private val logger = LoggerFactory.getLogger(classOf[RedisBackend])

  logger.info(s"Using redis backend at $host:$port")
  protected def client = new RedisClient(host, port)

  // Use namespace as a prefix to provide a concrete implementation of RedisSupport methods
  protected def rget(key: String)(implicit c: RedisClient): Option[String] = {
    c.get[String](s"$namespace:$key")
  }
  protected def rset(key: String, data: String)(implicit c: RedisClient): Boolean = {
    c.set(s"$namespace:$key", data)
  }
  protected def rdel(key: String)(implicit c: RedisClient): Option[Long] = c.del(
    s"$namespace:$key"
  )
  protected def rscan(cursor: Int, count: Int = 10)(implicit c: RedisClient): Option[
    (Option[Int], Option[List[Option[String]]])
  ] = c.scan[String](cursor, s"$namespace:*", count)
  protected def rmget(key: String, keys: String*)(
    implicit c: RedisClient
  ): Option[List[Option[String]]] = {
    c.mget[String](s"$namespace:$key", (keys map { k: String => s"$namespace:$k" }): _*)
  }

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

    rget(name)(c) map {
      parseJson(_).extract[Condition]
    }
  }

  /**
   * Get a whole batch of switch configs with a single redis MGET call
   */
  private def switchConfigs(names: String*)(implicit c: RedisClient): Future[
    List[Option[Condition]]
  ] = Future {
    logger.info(s"Getting config for a batch of ${names.length} switches")

    names match {
      case head :: tail => rmget(head, tail: _*) map {
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
    rdel(name)(client)
  }

  def configureSwitch(name: String, condition: Condition): Future[Unit] = Future {
    logger.info(s"Setting config for switch $name")
    validateName(name)

    val data = writeJson(condition)
    rset(name, data)(client)
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
    implicit val c = client

    def fetchKeys(cursor: Int = 0, acc: List[String] = Nil): Future[List[String]] = {
      val res: Future[Option[(Option[Int], Option[List[Option[String]]])]] = Future(rscan(cursor))

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
        batch: List[String] => switchConfigs(batch: _*) map {
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
