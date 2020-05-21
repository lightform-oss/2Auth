package com.lightform._2auth.services

import java.security.SecureRandom
import java.time.{Clock, Duration, Instant}
import java.{util => ju}

import black.door.jose.Mapper
import black.door.jose.json.playjson.JsonSupport._
import black.door.jose.jwk.Jwk
import black.door.jose.jwt.{Claims, Jwt, JwtValidator}
import cats.Monad
import cats.data.OptionT
import cats.implicits._
import com.lightform._2auth.scalaapi.interfaces.AuthorizationCodeService
import com.lightform._2auth.scalaapi.models.AuthorizationCodeMeta
import com.lightform._2auth.services.JwtAuthorizationCodeService.{
  randomString,
  AuthorizationCodeRepository,
  AuthzCodeClaims
}
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.Json

object JwtAuthorizationCodeService {

  case class AuthzCodeClaims(
      sub: String,
      aud: String,
      jti: String,
      uri: Option[String],
      scp: Option[Set[String]]
    )

  object AuthzCodeClaims {
    implicit val format = Json.format[AuthzCodeClaims]
  }

  trait AuthorizationCodeRepository[F[_]] {
    def isCodeUsed(id: String): F[Boolean]
    def setCodeUsed(id: String): F[Unit]
  }

  private val random =
    ThreadLocal.withInitial[ju.Random](() => new SecureRandom())

  def randomString: String = {
    val id = new Array[Byte](16)
    random.get.nextBytes(id)

    ju.Base64.getUrlEncoder.withoutPadding.encodeToString(id)
  }
}

class JwtAuthorizationCodeService[F[+_]: Monad](
    key: Jwk,
    validityDuration: Duration,
    repo: AuthorizationCodeRepository[F]
  )(
    implicit clock: Clock
  ) extends AuthorizationCodeService[F]
    with LazyLogging {

  def generateCode(
      userId: String,
      clientId: String,
      redirectUri: Option[String],
      scope: Set[String]
    ): F[String] = {
    val codeId = randomString
    val code = Jwt.sign(
      Claims(
        exp = Some(Instant.now(clock).plus(validityDuration)),
        unregistered = AuthzCodeClaims(
          userId,
          clientId,
          codeId,
          redirectUri,
          if (scope.isEmpty) None else Some(scope)
        )
      ),
      key
    )

    code.pure[F]
  }

  def validateCode(code: String): F[Option[AuthorizationCodeMeta]] = {
    implicit val imp = implicitly[Mapper[Array[Byte], Claims[AuthzCodeClaims]]]
    Jwt
      .validate(code)[AuthzCodeClaims]
      .using(key, fallbackJwtValidator = JwtValidator.defaultValidator(clock))
      .now match {
      case Left(problem) =>
        logger.debug("Could not validate authz code: " + problem)
        None.pure[F]
      case Right(Jwt(_, c)) =>
        val AuthzCodeClaims(userId, clientId, codeId, uri, scope) =
          c.unregistered
        (for {
          _ <- OptionT(
            repo.isCodeUsed(codeId).map(used => if (used) None else Some(()))
          )
          _ <- OptionT.liftF(repo.setCodeUsed(codeId))
        } yield AuthorizationCodeMeta(
          userId,
          clientId,
          uri,
          scope.getOrElse(Set.empty)
        )).value
    }
  }

}
