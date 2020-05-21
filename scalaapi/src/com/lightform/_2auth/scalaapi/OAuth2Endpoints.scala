package com.lightform._2auth.scalaapi

import cats.MonadError
import cats.data.EitherT
import cats.implicits._
import com.lightform._2auth.javaapi.interfaces.AuthorizationGrant.{CodeGrant, TokenGrant}
import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenCodeRequest,
  AccessTokenPasswordRequest,
  AccessTokenRefreshRequest,
  AccessTokenRequest,
  AuthorizationRequest,
  AccessTokenResponse => AccessTokenResponseI,
  AuthorizationErrorResponse => AuthorizationErrorResponseI,
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
import com.lightform._2auth.scalaapi.payloads.responses.{
  AuthorizationCodeResponse,
  AuthorizationErrorResponse,
  ErrorResponse
}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class OAuth2Endpoints[F[+_]](
    userService: UserService[F],
    tokenService: TokenService[F],
    clientService: ClientService[F],
    codeService: AuthorizationCodeService[F]
  )(
    implicit F: MonadError[F, Throwable]
  ) extends AbstractOAuth2Service[F] {

  def handleAuthorizationRequest(
      userId: String,
      request: AuthorizationRequest
    ): F[Either[AuthorizationErrorResponseI, AuthorizationResponseI]] =
    (request.getResponseType match {
      case "token" => handleImplicitRequest(userId, request)
      case "code" =>
        handleCodeAuthzRequest(userId, request)
    }).recover {
      case e: ErrorResponse =>
        Left(AuthorizationErrorResponse(e, request.getState.toScala, None, false, false))
    }

  def handleImplicitRequest(
      userId: String,
      request: AuthorizationRequest
    ): F[Either[AuthorizationErrorResponseI, AuthorizationResponseI]] =
    Left(
      AuthorizationErrorResponse(
        ErrorResponse(UNSUPPORTED_RESPONSE_TYPE, None),
        request.getState.toScala,
        request.getRedirectUri.toScala,
        false,
        true
      )
    ).pure[F]

  def handleCodeAuthzRequest(
      userId: String,
      request: AuthorizationRequest
    ): F[Either[AuthorizationErrorResponse, AuthorizationCodeResponse]] =
    (for {
      maybeRequiredRedirectUri <- EitherT.right[AuthorizationErrorResponse](
        clientService.retrieveClientRedirectUri(request.getClientId)
      )
      redirectUri <- EitherT.fromEither[F](
        (maybeRequiredRedirectUri, request.getRedirectUri.toScala) match {
          case (Some(registeredUri), Some(providedUri)) if registeredUri == providedUri =>
            Right(registeredUri)
          case (Some(_), Some(_)) =>
            Left(
              AuthorizationErrorResponse(
                ErrorResponse(
                  INVALID_REQUEST,
                  Some(
                    "Provided redirect_uri does not match registered redirect_uri."
                  )
                ),
                None,
                None,
                false,
                false
              )
            )
          case (Some(redirectUri), None) => Right(redirectUri)
          case (None, Some(redirectUri)) => Right(redirectUri)
          case (None, None) =>
            Left(
              AuthorizationErrorResponse(
                ErrorResponse(INVALID_REQUEST, Some("Missing redirect_uri.")),
                None,
                None,
                false,
                false
              )
            )
        }
      )
      state = request.getState.toScala
      code <- EitherT.right[AuthorizationErrorResponse](
        codeService.generateCode(
          userId,
          request.getClientId,
          request.getRedirectUri.toScala,
          request.getScope.asScala.toSet
        )
      )
    } yield AuthorizationCodeResponse(redirectUri, code, state)).value

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
                if (_) // token was issued to the client who's currently authenticating
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
          case (Some(requiredUri), Some(providedUri)) if requiredUri == providedUri =>
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
