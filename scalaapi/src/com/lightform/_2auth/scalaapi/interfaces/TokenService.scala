package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenResponse,
  RefreshTokenMeta
}

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
