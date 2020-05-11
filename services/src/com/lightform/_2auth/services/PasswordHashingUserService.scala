package com.lightform._2auth.services

import com.lightform._2auth.scalaapi.interfaces.UserService
import de.mkammerer.argon2.Argon2
import de.mkammerer.argon2.Argon2Helper
import de.mkammerer.argon2.Argon2Factory
import java.nio.charset.StandardCharsets.UTF_8
import cats.data.OptionT
import com.lightform._2auth.services.PasswordHashingUserService.UserRepository
import cats.Functor

class PasswordHashingUserService[F[_]: Functor](
    repo: UserRepository[F],
    iterations: Int = 20,
    memory: Int = 65536,
    parallelism: Int = Runtime.getRuntime.availableProcessors
) extends UserService[F] {
  private val argon2 = Argon2Factory.create()

  def authenticateUser(username: String, password: String): F[Option[String]] =
    OptionT(repo.retrieveUser(username))
      .filter(meta =>
        argon2.verify(meta.passwordHash, password.getBytes(UTF_8))
      )
      .map(_.id)
      .value

  def hash(password: String) =
    argon2.hash(iterations, memory, parallelism, password.getBytes(UTF_8))
}

object PasswordHashingUserService {
  case class UserMeta(id: String, passwordHash: String)

  trait UserRepository[F[_]] {
    def retrieveUser(username: String): F[Option[UserMeta]]
  }
}
