package com.xantoria.flippy.db

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import com.redis.RedisClient
import net.liftweb.json.{Formats, parse => parseJson}
import net.liftweb.json.Serialization.{write => writeJson}
import org.slf4j.{Logger, LoggerFactory}

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
  protected val namespace: String
  protected val logger = LoggerFactory.getLogger(classOf[RedisSupport])

  protected def rget(key: String)(implicit c: RedisClient): Option[String]
  protected def rset(key: String, data: String)(implicit c: RedisClient): Boolean
  protected def rdel(key: String)(implicit c: RedisClient): Option[Long]

  // Yo dawg, I herd you liek options...
  protected def rscan(cursor: Int, count: Int = 10)(implicit c: RedisClient): Option[
    (Option[Int], List[(String, String)])
  ]
  protected def rmget(key: String, keys: String*)(
    implicit c: RedisClient
  ): Option[List[Option[String]]]
}

/**
 * RedisSupport variant which operates on at a database level, i.e. across the whole keyspace
 *
 * Redis commands are implemented as basic GET, SET, DEL, SCAN etc., using the namespace as a
 * key prefix.
 */
trait RedisDatabaseLevelSupport extends RedisSupport {

  /**
   * The SCAN command only returns redis keys, so transform the result and supplement with an MGET
   */
  protected def supplementScan(
    results: (Option[Int], Option[List[Option[String]]])
  )(implicit c: RedisClient): (Option[Int], List[(String, String)]) = {
    val (cursor, entries) = results
    (
      cursor,
      entries flatMap {
        res: List[Option[String]] => res.flatten map { _.drop(namespace.length + 1) } match {
          case Nil => None
          case head :: tail => rmget(head, tail: _*) map {
            values: List[Option[String]] => ((head +: tail) zip values) collect {
              case (k, Some(v)) => (k, v)
            }
          }
        }
      } getOrElse Nil
    )
  }

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
    (Option[Int], List[(String, String)])
  ] = c.scan[String](cursor, s"$namespace:*", count) map supplementScan

  protected def rmget(key: String, keys: String*)(
    implicit c: RedisClient
  ): Option[List[Option[String]]] = {
    c.mget[String](s"$namespace:$key", (keys map { k: String => s"$namespace:$k" }): _*)
  }
}

/**
 * RedisSupport variant which operates on a redis hash for the key set
 *
 * Redis commands are implemented as HGET, HSET, etc., using namespace as the key of the hash elem
 */
trait RedisHashLevelSupport extends RedisSupport {
  private def transformScan(
    results: (Option[Int], Option[List[Option[String]]])
  ): (Option[Int], List[(String, String)]) = {
    val (cursor, entries) = results
    (
      cursor,
      entries.map {
        _.grouped(2).collect { case Some(k) :: Some(v) :: Nil => (k, v) }.toList
      } getOrElse Nil
    )
  }

  protected def rget(key: String)(implicit c: RedisClient): Option[String] = c.hget[String](
    namespace, key
  )
  protected def rset(key: String, data: String)(implicit c: RedisClient): Boolean = c.hset(
    namespace, key, data
  )
  protected def rdel(key: String)(implicit c: RedisClient): Option[Long] = c.hdel(
    namespace, key
  )

  protected def rscan(cursor: Int, count: Int = 10)(implicit c: RedisClient): Option[
    (Option[Int], List[(String, String)])
  ] = c.hscan[String](key = namespace, cursor = cursor, count = count) map transformScan

  protected def rmget(key: String, keys: String*)(
    implicit c: RedisClient
  ): Option[List[Option[String]]] = {
    val allKeys = key +: keys

    // Just to be annoying, hmget returns a very different type to regular mget
    c.hmget[String, String](namespace, allKeys: _*) map {
      results: Map[String, String] => allKeys.map {
        k: String => results.get(k)
      }.toList
    }
  }
}

trait RedisBackendHandling extends Backend {
  this: RedisSupport =>

  protected val host: String
  protected val port: Int
  protected val batchRetrievalSize: Int
  protected implicit val ec: ExecutionContext
  protected implicit val formats: Formats

  private final val ALLOWED_SWITCH_PATTERN = """^[\w-_]{1,64}$""".r

  protected def client = new RedisClient(host, port)

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

    def fetch(
      cursor: Int = 0,
      acc: List[(String, String)] = Nil
    ): Future[List[(String, String)]] = {
      val res: Future[Option[(Option[Int], List[(String, String)])]] = Future(rscan(cursor))

      res flatMap {
        _ map {
          case (Some(cur), result) if cur != 0 => fetch(cur, acc ++ result)
          case (_, result) => Future(acc ++ result)
        } getOrElse { Future(Nil) }
      }
    }

    // Sadly the from/to has to be applied after scanning everything because of sorting
    val from = offset getOrElse 0
    val to = limit map { _ + from }
    fetch() map {
      s: List[(String, String)] => s.sorted.slice(from, to getOrElse s.length) map {
        case (k, v) => (k, parseJson(v).extract[Condition])
      }
    }
  }
}

class RedisBackend(
  override protected val host: String,
  override protected val port: Int,
  override protected val namespace: String,
  override protected val batchRetrievalSize: Int = 50
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends RedisBackendHandling with RedisDatabaseLevelSupport {
  logger.info(s"Using redis backend at $host:$port with global keyspace, prefix $namespace")
}

class RedisHashBackend(
  override protected val host: String,
  override protected val port: Int,
  override protected val namespace: String,
  override protected val batchRetrievalSize: Int = 50
)(
  implicit val ec: ExecutionContext,
  implicit val formats: Formats
) extends RedisBackendHandling with RedisHashLevelSupport {
  logger.info(s"Using redis backend at $host:$port with hash $namespace")
}
