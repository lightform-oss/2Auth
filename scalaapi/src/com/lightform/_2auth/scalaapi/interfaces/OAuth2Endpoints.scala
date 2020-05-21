package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces._

trait OAuth2Endpoints[F[_]] {

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
    ): F[Either[AuthorizationErrorResponse, AuthorizationResponse]]

  def handleTokenRequest(
      request: AccessTokenRequest,
      clientId: Option[String],
      clientSecret: Option[String]
    ): F[Either[ErrorResponse, AccessTokenResponse]]
}
