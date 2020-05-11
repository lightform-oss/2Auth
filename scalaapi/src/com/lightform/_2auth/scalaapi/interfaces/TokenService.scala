package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.{AccessTokenResponse, RefreshTokenMeta}

trait TokenService[F[_]] {

  /**
    * @param userId only None for client_credentials grant
    * @param clientId Some for implicit flows or confidential clients
    * @param confidentialClient true if the client was authenticated
    * @param refreshToken the refresh token used if refresh_token grant
    * @param scope the scope requested by the client, or empty if none was requested
    * @return an access token and optionally a refresh token
    */
  def createToken(
      userId: Option[String],
      clientId: Option[String],
      confidentialClient: Boolean,
      refreshToken: Option[String],
      scope: Set[String]
    ): F[AccessTokenResponse]

  def validateRefreshToken(token: String): F[Option[RefreshTokenMeta]]
}

trait GrantInference[F[_]] {
  this: TokenService[F] =>

  def createToken(
      maybeUserId: Option[String],
      maybeClientId: Option[String],
      confidentialClient: Boolean,
      maybeRefreshToken: Option[String],
      scope: Set[String]
    ) = (maybeUserId, maybeClientId, maybeRefreshToken) match {
    case (Some(userId), None, None) => createPasswordToken(userId, scope)
    case (None, Some(clientId), None) if confidentialClient =>
      createClientToken(clientId, scope)
    case (Some(userId), Some(clientId), None) if confidentialClient =>
      createCodeToken(userId, clientId, scope)
    case (Some(userId), Some(clientId), None) if !confidentialClient =>
      createImplicitToken(userId, clientId, scope)
    case (_, _, Some(refreshToken)) => createRefreshToken(refreshToken, scope)
    case _ =>
      createDefaultToken(
        maybeUserId,
        maybeClientId,
        confidentialClient,
        maybeRefreshToken,
        scope
      )
  }

  def createPasswordToken(userId: String, scope: Set[String]): F[AccessTokenResponse]
  def createClientToken(clientId: String, scope: Set[String]): F[AccessTokenResponse]

  def createCodeToken(
      userId: String,
      clientId: String,
      scope: Set[String]
    ): F[AccessTokenResponse]

  def createImplicitToken(
      userId: String,
      clientId: String,
      scope: Set[String]
    ): F[AccessTokenResponse]
  def createRefreshToken(refreshToken: String, scope: Set[String]): F[AccessTokenResponse]

  def createDefaultToken(
      userId: Option[String],
      clientId: Option[String],
      confidentialClient: Boolean,
      refreshToken: Option[String],
      scope: Set[String]
    ): F[AccessTokenResponse]
}
