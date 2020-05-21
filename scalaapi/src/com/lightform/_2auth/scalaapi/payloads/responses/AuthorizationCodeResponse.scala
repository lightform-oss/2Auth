package com.lightform._2auth.scalaapi.payloads.responses

import com.lightform._2auth.javaapi.interfaces
import scala.jdk.OptionConverters._

case class AuthorizationCodeResponse(redirectUri: String, code: String, state: Option[String])
    extends interfaces.AuthorizationGrant.CodeGrant
    with interfaces.AuthorizationResponse {

  def getRedirectUri = redirectUri
  def getCode        = code
  def getState       = state.toJava
  def getGrant       = this
}
