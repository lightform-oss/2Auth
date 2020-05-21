package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.AuthorizationGrant.CodeGrant
import com.lightform._2auth.javaapi.interfaces.{AuthorizationCodeMeta, AuthorizationGrant}

trait AuthorizationCodeService[F[_]] {

  /**
    * @param userId
    * @param clientId
    * @param redirectUri the redirect_uri, if one was provided by the client
    * @param scope
    * @return an authorization code that can be validated later
    */
  def generateCode(
      userId: String,
      clientId: String,
      redirectUri: Option[String],
      scope: Set[String]
    ): F[String]

  /**
    *
    * @param code
    * @return None if the provided code does not exist, is expired, has already been used, etc
    */
  def validateCode(code: String): F[Option[AuthorizationCodeMeta]]
}
