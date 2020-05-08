package com.lightform._2auth.scalaapi

import cats.MonadError
import cats.data.EitherT
import cats.implicits._
import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenCodeRequest,
  AccessTokenPasswordRequest,
  AccessTokenRefreshRequest,
  AccessTokenRequest,
  AuthorizationRequest,
  LimitedAccessTokenResponse,
  AccessTokenResponse => AccessTokenResponseI,
  AuthorizationResponse => AuthorizationResponseI,
  ErrorResponse => ErrorResponseI
}
import com.lightform._2auth.javaapi.models.Error._
import com.lightform._2auth.scalaapi.interfaces.{
  AuthorizationCodeService,
  ClientService,
  TokenService,
  UserService,
  OAuth2Endpoints => AbstractOAuth2Service
}
import com.lightform._2auth.scalaapi.payloads.responses.ErrorResponse

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class OAuth2Endpoints[F[+_]](
    userService: UserService[F],
    tokenService: TokenService[F],
    clientService: ClientService[F],
    codeService: AuthorizationCodeService[F]
)(implicit F: MonadError[F, Throwable])
    extends AbstractOAuth2Service[F] {

  def handleAuthorizationRequest(
      userId: String,
      request: AuthorizationRequest
  ): F[
    Either[ErrorResponseI, Either[
      LimitedAccessTokenResponse,
      AuthorizationResponseI
    ]]
  ] =
    (request.getResponseType match {
      case "token" => handleImplicitRequest(userId, request).map(_.map(Left(_)))
      case "code" =>
        handleCodeAuthzRequest(userId, request).map(_.map(Right(_)))
    }).recover { case e: ErrorResponse => Left(e) }

  def handleImplicitRequest(
      userId: String,
      request: AuthorizationRequest
  ): F[Either[ErrorResponse, LimitedAccessTokenResponse]] =
    Left(ErrorResponse(UNSUPPORTED_RESPONSE_TYPE, None)).pure[F]

  def handleCodeAuthzRequest(
      userId: String,
      request: AuthorizationRequest
  ): F[Either[ErrorResponseI, AuthorizationResponseI]] =
    (for {
      maybeRequiredRedirectUri <- EitherT.right[ErrorResponse](
        clientService.retrieveClientRedirectUri(request.getClientId)
      )
      _ <- EitherT.fromEither[F](
        (maybeRequiredRedirectUri, request.getRedirectUri.toScala) match {
          case (Some(registeredUri), Some(providedUri))
              if registeredUri == providedUri =>
            Right(())
          case (Some(_), Some(_)) =>
            Left(
              ErrorResponse(
                INVALID_REQUEST,
                Some(
                  "Provided redirect_uri does not match registered redirect_uri."
                )
              )
            )
          case (Some(_), None) => Right(())
          case (None, Some(_)) => Right(())
          case (None, None) =>
            Left(ErrorResponse(INVALID_REQUEST, Some("Missing redirect_uri.")))
        }
      )
      code <- EitherT.right[ErrorResponse](
        codeService.generateCode(
          userId,
          request.getClientId,
          request.getRedirectUri.toScala,
          request.getScope.asScala.toSet,
          request.getState.toScala
        )
      )
    } yield code).value

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
      case code: AccessTokenCodeRequest =>
        (clientId, clientSecret) match {
          case (Some(id), Some(sec)) => handleCodeTokenRequest(code, id, sec)
          case _ =>
            Left(
              ErrorResponse(
                INVALID_CLIENT,
                Some(
                  "Client authentication failed (no client authentication included, or unsupported authentication method)."
                )
              )
            ).pure[F]
        }
      case _ => Left(ErrorResponse(UNSUPPORTED_GRANT_TYPE, None)).pure[F]
    }).recover { case e: ErrorResponse => Left(e) }

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
        tokenService.createToken(
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
        tokenService.validateRefreshToken(request.getRefreshToken),
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
               clientService
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
        tokenService.createToken(
          meta.getUserId.toScala,
          meta.getClientId.toScala,
          meta.isConfidentialClient,
          Some(request.getRefreshToken),
          request.getScope.asScala.toSet
        )
      )
    } yield newToken).value

  def handleCodeTokenRequest(
      request: AccessTokenCodeRequest,
      clientId: String,
      clientSecret: String
  ): F[Either[ErrorResponse, AccessTokenResponseI]] =
    (for {
      // get information about the authorization code
      meta <- EitherT.fromOptionF(
        codeService.validateCode(request.getCode),
        ErrorResponse(
          INVALID_GRANT,
          Some(
            "The provided authorization grant (authorization code) is invalid, expired, or revoked."
          )
        )
      )
      // ensure the client is valid
      _ <- EitherT(
        if (meta.getClientId == clientId)
          clientService
            .validateClient(meta.getClientId, clientSecret)
            .map(validClient =>
              if (!validClient)
                Left(
                  ErrorResponse(
                    INVALID_CLIENT,
                    Some("Client authentication failed.")
                  )
                )
              else Right(())
            )
        else
          Left(
            ErrorResponse(
              INVALID_GRANT,
              Some(
                "The provided authorization grant (authorization code) was issued to another client."
              )
            )
          ).pure[F]
      )
      // ensure redirect URIs match
      _ <- EitherT.fromEither[F](
        (meta.getRedirectUri.toScala, request.getRedirectUri.toScala) match {
          // correct redirect uri was provided
          case (Some(requiredUri), Some(providedUri))
              if requiredUri == providedUri =>
            Right(())
          // redirect uri was expected, but was not provided or did not match required value
          case (Some(_), _) =>
            Left(
              ErrorResponse(
                INVALID_GRANT,
                Some(
                  "The provided authorization grant (authorization code) does not match the redirection URI used in the authorization request"
                )
              )
            )
          // redirect uri was not required
          case (None, _) => Right(())
        }
      )
      // issue a token
      token <- EitherT.right[ErrorResponse](
        tokenService.createToken(
          Some(meta.getUserId),
          Some(meta.getClientId),
          true,
          None,
          meta.getScope.asScala.toSet
        )
      )
    } yield token).value
}
