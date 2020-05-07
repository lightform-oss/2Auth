package com.lightform._2auth.scalaapi.payloads.requests

import com.lightform._2auth.javaapi.interfaces
import scala.jdk.CollectionConverters._

case class AccessTokenPasswordRequest(
    grant_type: "password",
    username: String,
    password: String,
    scope: Set[String] = Set.empty
) extends interfaces.AccessTokenPasswordRequest {
  def getUsername = username
  def getPassword = password
  def getScope    = scope.asJava
}
