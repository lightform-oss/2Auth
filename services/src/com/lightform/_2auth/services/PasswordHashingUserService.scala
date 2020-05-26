package com.lightform._2auth.services

import java.nio.charset.StandardCharsets.UTF_8
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import java.util.{TimerTask, Timer => JTimer}

import cats.{ApplicativeError, Functor}
import cats.data.OptionT
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import com.lightform._2auth.scalaapi.interfaces.UserService
import com.lightform._2auth.scalaapi.payloads.responses.ErrorResponse
import com.lightform._2auth.services.PasswordHashingUserService.UserRepository
import de.mkammerer.argon2.Argon2Factory

import scala.concurrent.duration._

class PasswordHashingUserService[F[_]: Functor](
    repo: UserRepository[F],
    iterations: Int = 20,
    memory: Int = 65536,
    parallelism: Int = Runtime.getRuntime.availableProcessors,
    rateLimiter: Option[RateLimiter[String]] = Some(
      new RateLimiter[String](10, 1 minute)
    )
)(implicit F: ApplicativeError[F, Throwable])
    extends UserService[F] {
  private val argon2 = Argon2Factory.create()

  def authenticateUser(
      username: String,
      password: String
  ): F[Option[String]] = {
    def checkPassword =
      OptionT(repo.retrieveUser(username))
        .filter(meta =>
          argon2.verify(meta.passwordHash, password.getBytes(UTF_8))
        )
        .map(_.id)
        .value

    rateLimiter match {
      case Some(rl) =>
        rl.attempt(username.toLowerCase)(checkPassword)
          .getOrElse(
            F.raiseError(
              ErrorResponse(
                () => "rate_limited",
                Some(
                  "Too many login attempts have been made too quickly for this user, try again later."
                ),
                Some("https://i.giphy.com/media/MF1aaZpwtmqUa2FoCa/source.gif")
              )
            )
          )
      case None => checkPassword
    }
  }

  def hash(password: String) =
    argon2.hash(iterations, memory, parallelism, password.getBytes(UTF_8))
}

object PasswordHashingUserService {
  case class UserMeta(id: String, passwordHash: String)

  trait UserRepository[F[_]] {
    def retrieveUser(username: String): F[Option[UserMeta]]
  }
}

/**
  * A local rate limiter.
  * The rate limiter loosely ensures that no more than threshold accesses are
  * permitted inside the window of time.
  *
  * @param threshold
  * @param window
  * @tparam K
  */
class RateLimiter[K](
    threshold: Int,
    window: FiniteDuration
) {

  private val timer = new JTimer(true)
  private val cache = CacheBuilder.newBuilder
    .expireAfterAccess(window.toSeconds, SECONDS)
    .build(new CacheLoader[K, AtomicInteger] {
      def load(key: K) = new AtomicInteger(0)
    })
    .asInstanceOf[LoadingCache[K, AtomicInteger]]

  /**
    * Testing does not count against the key's quota.
    * @return false if a key is currently rate limited.
    */
  def test(k: K) =
    cache.asMap.getOrDefault(k, new AtomicInteger(0)).get < threshold

  def contains(k: K) = cache.asMap.containsKey(k)

  /**
    * Performing the action counts against the key's quota.
    * @return Some result of the action if the key is not rate limited.
    *         If none, the action was not invoked.
    */
  def attempt[A](k: K)(action: => A): Option[A] = {
    val counter = cache.get(k)
    if (counter.get < threshold) {
      counter.incrementAndGet()
      timer.schedule(
        new TimerTask {
          def run() = cache.get(k).decrementAndGet()
        },
        window.toMillis
      )
      Some(action)
    } else None
  }
}
