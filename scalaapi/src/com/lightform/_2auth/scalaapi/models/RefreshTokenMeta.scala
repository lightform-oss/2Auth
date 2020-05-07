package com.lightform._2auth.scalaapi.models

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import com.lightform._2auth.javaapi.interfaces

final case class RefreshTokenMeta(
    userId: Option[String],
    clientId: Option[String],
    confidentialClient: Boolean,
    scope: Set[String] = Set.empty
) extends interfaces.RefreshTokenMeta {

  def getUserId            = userId.toJava
  def getClientId          = clientId.toJava
  def isConfidentialClient = confidentialClient
  def getScope             = scope.asJava

}
