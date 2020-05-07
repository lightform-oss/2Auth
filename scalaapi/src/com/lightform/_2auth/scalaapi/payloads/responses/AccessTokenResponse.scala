package com.lightform._2auth.scalaapi.payloads.responses

import java.time.Duration

import com.lightform._2auth.javaapi.interfaces

import scala.jdk.OptionConverters._
import scala.jdk.CollectionConverters._

case class AccessTokenResponse(
    access_token: String,
    token_type: String,
    expires_in: Option[Duration],
    refresh_token: Option[String],
    scope: Set[String] = Set.empty
) extends interfaces.AccessTokenResponse {
  def getAccessToken  = access_token
  def getTokenType    = token_type
  def getExpiresIn    = expires_in.toJava
  def getRefreshToken = refresh_token.toJava
  def getScope        = scope.asJava
}
