package com.lightform._2auth.scalaapi.services

import com.lightform._2auth.scalaapi.models.{RefreshTokenMeta}
import com.lightform._2auth.scalaapi.interfaces.{
  ClientRepository,
  TokenRepository,
  UserRepository
}

import scala.util.Try
import scala.util.Success
import com.lightform._2auth.scalaapi.payloads.responses.AccessTokenResponse
import java.{util => ju}
import com.lightform._2auth.scalaapi.interfaces.AuthorizationCodeRepository
import com.lightform._2auth.scalaapi.payloads.responses.AuthorizationResponse
import com.lightform._2auth.scalaapi.models.AuthorizationCodeMeta

class InMemoryBackend(
    clientId: String,
    clientSecret: String,
    userId: String,
    username: String,
    password: String,
    redirectUri: String
) extends ClientRepository[Try]
    with TokenRepository[Try]
    with UserRepository[Try]
    with AuthorizationCodeRepository[Try] {
  var redirectIsRegistered = true

  var code: String                         = null
  var requestedRedirectUri: Option[String] = null
  var requestedScope: Set[String]          = null

  def retrieveClientRedirectUri(clientId: String) =
    Success(
      Some(redirectUri).filter(_ =>
        clientId == this.clientId && redirectIsRegistered
      )
    )

  def generateCode(
      userId: String,
      clientId: String,
      redirectUri: Option[String],
      scope: Set[String],
      state: Option[String]
  ): Try[AuthorizationResponse] = {
    this.code = ju.UUID.randomUUID.toString
    this.requestedRedirectUri = redirectUri
    requestedScope = scope
    Success(AuthorizationResponse(code, state))
  }

  def validateCode(code: String): Try[Option[AuthorizationCodeMeta]] =
    Success(
      Option(
        AuthorizationCodeMeta(
          userId,
          clientId,
          requestedRedirectUri,
          requestedScope
        )
      ).filter(_ => code == this.code)
    )

  var refreshToken           = ""
  var meta: RefreshTokenMeta = null

  def validateClient(clientId: String, clientSecret: String) =
    Success(clientId == this.clientId && clientSecret == this.clientSecret)

  def createToken(
      userId: Option[String],
      clientId: Option[String],
      confidentialClient: Boolean,
      refreshToken: Option[String],
      scope: Set[String]
  ) = {
    //(userId, clientId, confidentialClient, refreshToken) match {
    /*
    // password
    case (Some(uid), None, false, None) =>
      this.refreshToken = ju.UUID.randomUUID().toString()
      Success(AccessTokenResponse(ju.UUID.randomUUID.toString, "Bearer", None, Some(this.refreshToken)))
    // implicit
    case (Some(uid), Some(cid), false, None) =>
      this.refreshToken = ju.UUID.randomUUID().toString()
      Success(AccessTokenResponse(ju.UUID.randomUUID.toString, "Bearer", None, Some(this.refreshToken)))
    // code
    case (Some(uid), Some(cid), true, None) =>
      this.refreshToken = ju.UUID.randomUUID().toString()
      Success(AccessTokenResponse(ju.UUID.randomUUID.toString, "Bearer", None, Some(this.refreshToken)))
    // client
    case (None, Some(cid), true, None) =>
    this.refreshToken = ju.UUID.randomUUID().toString()
      Success(AccessTokenResponse(ju.UUID.randomUUID.toString, "Bearer", None, Some(this.refreshToken)))
    // refresh /w ratchet
    case (Some(uid), _, false, Some(refresh)) =>
    this.refreshToken = ju.UUID.randomUUID().toString()
      Success(AccessTokenResponse(ju.UUID.randomUUID.toString, "Bearer", None, Some(this.refreshToken)))
    // refresh
    case (Some(uid), _, true, Some(refresh)) =>
     */
    this.refreshToken = ju.UUID.randomUUID().toString()
    this.meta = RefreshTokenMeta(userId, clientId, confidentialClient, scope)
    Success(
      AccessTokenResponse(
        ju.UUID.randomUUID.toString,
        "Bearer",
        None,
        Some(this.refreshToken)
      )
    )
  }

  def validateRefreshToken(token: String) =
    Success(Option(meta).filter(_ => token == refreshToken))

  def authenticateUser(
      username: String,
      password: String
  ) =
    Success(
      Option(userId).filter(_ =>
        username == this.username && password == this.password
      )
    )
}
