package com.lightform._2auth.scalaapi.payloads.requests

import scala.jdk.CollectionConverters._
import com.lightform._2auth.javaapi.interfaces

final case class AccessTokenRefreshRequest(
    grant_type: "refresh_token",
    refresh_token: String,
    scope: Set[String] = Set.empty
) extends interfaces.AccessTokenRefreshRequest {

  def getRefreshToken = refresh_token
  def getScope        = scope.asJava
}
