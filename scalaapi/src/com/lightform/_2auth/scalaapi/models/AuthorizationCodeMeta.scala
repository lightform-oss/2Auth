package com.lightform._2auth.scalaapi.models

import com.lightform._2auth.javaapi.interfaces
import java.{util => ju}
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

final case class AuthorizationCodeMeta(
    userId: String,
    clientId: String,
    redirectUri: Option[String],
    scope: Set[String]
) extends interfaces.AuthorizationCodeMeta {

  def getUserId      = userId
  def getClientId    = clientId
  def getRedirectUri = redirectUri.toJava
  def getScope       = scope.asJava

}
