package com.lightform._2auth.scalaapi.interfaces

import com.lightform._2auth.javaapi.interfaces.AuthorizationCodeMeta
import com.lightform._2auth.javaapi.interfaces.AuthorizationResponse

trait AuthorizationCodeService[F[_]] {

  /**
    * @param userId
    * @param clientId
    * @param redirectUri
    * @param scope
    * @param state must be copied to the response
    * @return an authorization code that can be validated later
    */
  def generateCode(
      userId: String,
      clientId: String,
      redirectUri: Option[String],
      scope: Set[String],
      state: Option[String]
  ): F[AuthorizationResponse]

  /**
    *
    * @param code
    * @return None if the provided code does not exist, is expired, has already been used, etc
    */
  def validateCode(code: String): F[Option[AuthorizationCodeMeta]]
}
