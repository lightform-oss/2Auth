package com.lightform._2auth.scalaapi.payloads.responses

import com.lightform._2auth.javaapi.interfaces
import scala.jdk.OptionConverters._

case class AuthorizationErrorResponse(
    error: ErrorResponse,
    state: Option[String],
    redirectUri: Option[String],
    isInQuery: Boolean,
    isInFragment: Boolean
  ) extends interfaces.AuthorizationErrorResponse {
  def getError       = error
  def getState       = state.toJava
  def getRedirectUri = redirectUri.toJava
}
