package com.lightform._2auth.scalaapi.services

import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenCodeRequest,
  AccessTokenPasswordRequest,
  AccessTokenRefreshRequest,
  AccessTokenRequest
}
import com.lightform._2auth.scalaapi.interfaces.{
  ClientRepository,
  TokenRepository,
  UserRepository,
  OAuth2Service => AbstractOAuth2Service
}
import com.lightform._2auth.scalaapi.payloads.responses.AccessTokenResponse
import com.lightform._2auth.javaapi.interfaces.{
  ErrorResponse => ErrorResponseI,
  AccessTokenResponse => AccessTokenResponseI
}
import cats.MonadError
import cats.data.EitherT
import com.lightform._2auth.javaapi.models.Error._
import cats.implicits._

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import com.lightform._2auth.scalaapi.payloads.responses.ErrorResponse

class OAuth2Service[F[+_]](
    userService: UserRepository[F],
    tokenRepository: TokenRepository[F],
    clientRepository: ClientRepository[F]
)(implicit F: MonadError[F, Throwable])
    extends AbstractOAuth2Service[F] {

  def handleTokenRequest(
      request: AccessTokenRequest,
      clientId: Option[String],
      clientSecret: Option[String]
  ): F[Either[ErrorResponseI, AccessTokenResponseI]] =
    (request match {
      case password: AccessTokenPasswordRequest =>
        handlePasswordRequest(password)
      case refresh: AccessTokenRefreshRequest =>
        handleRefreshRequest(refresh, clientId, clientSecret)
      //case code: AccessTokenCodeRequest => handleCodeRequest(code)
    }).recover{case e: ErrorResponse => Left(e)}

  def handlePasswordRequest(
      request: AccessTokenPasswordRequest
  ): F[Either[ErrorResponse, AccessTokenResponseI]] =
    (for {
      userId <- EitherT.fromOptionF(
        userService.authenticateUser(request.getUsername, request.getPassword),
        ErrorResponse(
          INVALID_GRANT,
          Some(
            "The provided authorization grant (resource owner credentials) is invalid."
          )
        )
      )
      token <- EitherT.right[ErrorResponse](
        tokenRepository.createToken(
          Some(userId),
          None,
          false,
          None,
          request.getScope.asScala.toSet
        )
      )
    } yield token).value

  def handleRefreshRequest(
      request: AccessTokenRefreshRequest,
      clientId: Option[String],
      clientSecret: Option[String]
  ): F[Either[ErrorResponse, AccessTokenResponseI]] =
    (for {
      meta <- EitherT.fromOptionF(
        tokenRepository.validateRefreshToken(request.getRefreshToken),
        ErrorResponse(
          INVALID_GRANT,
          Some("The provided refresh token is invalid, expired, or revoked")
        )
      )
      _ <- (
          meta.getClientId.toScala,
          for { id <- clientId; sec <- clientSecret } yield id -> sec
      ) match {
        // token wasn't issued to a confidential client
        case _ if !meta.isConfidentialClient =>
          EitherT.rightT[F, ErrorResponse](())

        // token was issued to a confidential client, but the caller hasn't provided client auth
        case (_, None) =>
          EitherT.leftT[F, Unit](
            ErrorResponse(
              INVALID_CLIENT,
              Some(
                "Client authentication failed (no client authentication included, or unsupported authentication method)."
              )
            )
          )

        case (Some(clientId), Some((providedId, providedSecret))) =>
          // log clientId == providedId
          EitherT(
            (if (clientId == providedId)
               clientRepository
                 .validateClient(clientId, providedSecret)
             else false.pure[F])
              .map(
                if (
                  _
                ) // token was issued to the client who's currently authenticating
                  Right(())
                else // token was issued to a confidential client, and the caller isn't them
                  Left(
                    ErrorResponse(
                      INVALID_CLIENT,
                      Some(
                        "Client authentication failed (invalid client_id or client_secret)."
                      )
                    )
                  )
              )
          )
      }
      newToken <- EitherT.right[ErrorResponse](
        tokenRepository.createToken(
          meta.getUserId.toScala,
          meta.getClientId.toScala,
          meta.isConfidentialClient,
          Some(request.getRefreshToken),
          request.getScope.asScala.toSet
        )
      )
    } yield newToken).value

  //def authorizationEndpoint(responseType: String, )
}
