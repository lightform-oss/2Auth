package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.AccessTokenRequest
import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenResponse,
  ErrorResponse
}

trait OAuth2Service[F[_]] {
  def handleTokenRequest(
      request: AccessTokenRequest,
      clientId: Option[String],
      clientSecret: Option[String]
  ): F[Either[ErrorResponse, AccessTokenResponse]]
}
