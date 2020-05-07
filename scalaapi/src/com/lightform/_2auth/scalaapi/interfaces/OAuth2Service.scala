package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.AccessTokenRequest
import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenResponse,
  ErrorResponse
}
import com.lightform._2auth.javaapi.interfaces.AuthorizationRequest
import com.lightform._2auth.javaapi.interfaces.LimitedAccessTokenResponse
import com.lightform._2auth.javaapi.interfaces.AuthorizationResponse

trait OAuth2Service[F[_]] {

  /**
    * This method assumes the user has already been authenticated and
    * approved the requested scopes
    * @param userId
    * @param request
    * @return
    */
  def handleAuthorizationRequest(
      userId: String,
      request: AuthorizationRequest
  ): F[Either[
    ErrorResponse,
    Either[LimitedAccessTokenResponse, AuthorizationResponse]
  ]]

  def handleTokenRequest(
      request: AccessTokenRequest,
      clientId: Option[String],
      clientSecret: Option[String]
  ): F[Either[ErrorResponse, AccessTokenResponse]]
}
