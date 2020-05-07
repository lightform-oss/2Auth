package com.lightform._2auth.scalaapi.services

import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenResponse,
  RefreshTokenMeta
}
import com.lightform._2auth.scalaapi.interfaces.{
  ClientRepository,
  TokenRepository,
  UserRepository
}

import scala.util.Try

class InMemoryBackend
    extends ClientRepository[Try]
    with TokenRepository[Try]
    with UserRepository[Try] {
  def validateClient(clientId: String, clientSecret: String): Try[Boolean] = ???

  def createToken(
      userId: Option[String],
      clientId: Option[String],
      confidentialClient: Boolean,
      refreshToken: Option[String],
      scope: Set[String]
  ): Try[AccessTokenResponse] = ???

  def validateRefreshToken(token: String): Try[Option[RefreshTokenMeta]] = ???

  def authenticateUser(
      username: String,
      password: String
  ): Try[Option[String]] = ???
}
