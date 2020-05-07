package com.lightform._2auth.scalaapi.payloads.requests

import com.lightform._2auth.javaapi.interfaces
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

case class AuthorizationRequest(
    response_type: String,
    client_id: String,
    redirect_uri: Option[String],
    scope: Set[String],
    state: Option[String]
) extends interfaces.AuthorizationRequest {
  def getResponseType = response_type
  def getClientId     = client_id
  def getRedirectUri  = redirect_uri.toJava
  def getScope        = scope.asJava
  def getState        = state.toJava
}
