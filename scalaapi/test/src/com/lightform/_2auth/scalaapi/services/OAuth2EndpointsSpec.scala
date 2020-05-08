package com.lightform._2auth.scalaapi.services

import java.time.{Clock, Duration}
import java.{util => ju}

import black.door.jose.jwk.P256KeyPair
import cats.implicits._
import com.lightform._2auth.javaapi.interfaces.AccessTokenRequest
import com.lightform._2auth.scalaapi.OAuth2Endpoints
import com.lightform._2auth.scalaapi.payloads.requests._
import com.lightform._2auth.services.JwtAuthorizationCodeService
import com.lightform._2auth.services.JwtAuthorizationCodeService.AuthorizationCodeRepository
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, TryValues}

import scala.util.{Success, Try}

class OAuth2EndpointsSpec
    extends AnyFlatSpec
    with Matchers
    with EitherValues
    with TryValues {

  "the handleTokenRequest method" should "respond to strange grants" in new fixtures {
    val strageRequest = new AccessTokenRequest {
      def getGrantType = "strange"
    }

    val Success(response) =
      service.handleTokenRequest(strageRequest, None, None)

    response.left.value.getError.getValue shouldEqual "unsupported_grant_type"
  }

  it should "recover oauth errors" in new fixtures {
    val response = service
      .handleTokenRequest(
        AccessTokenPasswordRequest("password", "boom", testPassword),
        None,
        None
      )
      .success
      .value
    response.left.value.getError.getValue shouldEqual "boom"
  }

  "Password grants" should "reject non-existent users" in new fixtures {
    val Success(response) = service.handleTokenRequest(
      AccessTokenPasswordRequest("password", "steve", testPassword),
      None,
      None
    )
    response.left.value.getError.getValue shouldEqual "invalid_grant"
  }

  it should "reject incorrect passwords" in new fixtures {
    val Success(response) = service.handleTokenRequest(
      AccessTokenPasswordRequest("password", testUsername, "abc123"),
      None,
      None
    )
    response.left.value.getError.getValue shouldEqual "invalid_grant"
  }

  it should "accept valid credentials" in new fixtures {
    val Success(response) = service.handleTokenRequest(
      AccessTokenPasswordRequest("password", testUsername, testPassword),
      None,
      None
    )
    response shouldBe 'right
  }

  "Refresh token grants" should "return new tokens" in new fixtures {
    val Success(response) = service.handleTokenRequest(
      AccessTokenPasswordRequest("password", testUsername, testPassword),
      None,
      None
    )

    val Success(refreshResponse) = service.handleTokenRequest(
      AccessTokenRefreshRequest(
        "refresh_token",
        response.right.value.getRefreshToken.get
      ),
      None,
      None
    )

    refreshResponse shouldBe 'right

    response.right.value.getAccessToken should not equal refreshResponse.right.value.getAccessToken
  }

  it should "reject bad refresh tokens" in new fixtures {
    val response = service
      .handleTokenRequest(
        AccessTokenPasswordRequest("password", testUsername, testPassword),
        None,
        None
      )
      .success
      .value

    val refreshResponse = service
      .handleTokenRequest(
        AccessTokenRefreshRequest(
          "refresh_token",
          "abc123"
        ),
        None,
        None
      )
      .success
      .value
      .left
      .value

    refreshResponse.getError.getValue shouldBe "invalid_grant"
  }

  it should "reject refresh tokens for confidential clients missing client auth" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .right
      .value

    val refreshResponse = service
      .handleTokenRequest(
        AccessTokenRefreshRequest(
          "refresh_token",
          tokenResponse.getRefreshToken.get
        ),
        None,
        None
      )
      .success
      .value
      .left
      .value

    refreshResponse.getError.getValue shouldEqual "invalid_client"
  }

  it should "reject refresh tokens for confidential clients with incorrect client ids" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .right
      .value

    val refreshResponse = service
      .handleTokenRequest(
        AccessTokenRefreshRequest(
          "refresh_token",
          tokenResponse.getRefreshToken.get
        ),
        Some("abc123"),
        Some(testClientSecret)
      )
      .success
      .value
      .left
      .value

    refreshResponse.getError.getValue shouldEqual "invalid_client"
  }

  it should "reject refresh tokens for confidential clients with incorrect client secrets" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .right
      .value

    val refreshResponse = service
      .handleTokenRequest(
        AccessTokenRefreshRequest(
          "refresh_token",
          tokenResponse.getRefreshToken.get
        ),
        Some(testClientId),
        Some("abc123")
      )
      .success
      .value
      .left
      .value

    refreshResponse.getError.getValue shouldEqual "invalid_client"
  }

  it should "accept refresh tokens for confiential clients with valid client auth" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .right
      .value

    val refreshResponse = service
      .handleTokenRequest(
        AccessTokenRefreshRequest(
          "refresh_token",
          tokenResponse.getRefreshToken.get
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value

    refreshResponse shouldBe 'right
  }

  "Authorization code grants" should "work" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value
    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
    tokenResponse shouldBe 'right
  }

  it should "reject incorrect client secrets" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value
    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some(testClientId),
        Some("abc123")
      )
      .success
      .value
      .left
      .value
    tokenResponse.getError.getValue shouldEqual "invalid_client"
  }

  it should "reject unknown clients" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value
    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        Some("steve"),
        Some(testClientSecret)
      )
      .success
      .value
      .left
      .value
    tokenResponse.getError.getValue shouldEqual "invalid_grant"
  }

  it should "reject missing client auth" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          None
        ),
        None,
        None
      )
      .success
      .value
      .left
      .value

    tokenResponse.getError.getValue shouldEqual "invalid_client"
  }

  it should "reject unknown codes" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .right
      .value
      .right
      .value
    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest("authorization_code", "abc123", None),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .left
      .value
    tokenResponse.getError.getValue shouldEqual "invalid_grant"
  }

  it should "ensure provided redirect URIs match registered URIs in auth requests" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest(
          "code",
          testClientId,
          Some("https://malice.example.com"),
          Set.empty,
          None
        )
      )
      .success
      .value
      .left
      .value

    authResponse.getError.getValue shouldEqual "invalid_request"
  }

  it should "ensure provided redirect URIs match expected URIs in token requests" in new fixtures {
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest(
          "code",
          testClientId,
          Some(testRedirectUri),
          Set.empty,
          None
        )
      )
      .success
      .value
      .right
      .value
      .right
      .value

    val tokenResponse = service
      .handleTokenRequest(
        AccessTokenCodeRequest(
          "authorization_code",
          authResponse.getCode,
          Some("https://malice.example.com")
        ),
        Some(testClientId),
        Some(testClientSecret)
      )
      .success
      .value
      .left
      .value

    tokenResponse.getError.getValue shouldEqual "invalid_grant"
  }

  it should "ensure redirect URIs are provided for clients without registered redirects" in new fixtures {
    backend.redirectIsRegistered = false
    val authResponse = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", testClientId, None, Set.empty, None)
      )
      .success
      .value
      .left
      .value

    authResponse.getError.getValue shouldEqual "invalid_request"
  }

  trait fixtures {
    implicit val clock = Clock.systemDefaultZone()

    val testClientId     = ju.UUID.randomUUID().toString()
    val testClientSecret = ju.UUID.randomUUID().toString()
    val testUserId       = ju.UUID.randomUUID().toString()
    val testUsername     = "john"
    val testPassword     = "bad password"
    val testRedirectUri  = "https://client.example.com/cb"

    val backend = new InMemoryBackend(
      testClientId,
      testClientSecret,
      testUserId,
      testUsername,
      testPassword,
      testRedirectUri
    )

    val codeService = new JwtAuthorizationCodeService(
      P256KeyPair.generate.withAlg(Some("ES256")),
      Duration.ofDays(1),
      new AuthorizationCodeRepository[Try] {
        def isCodeUsed(id: String)  = Success(false)
        def setCodeUsed(id: String) = Success(())
      }
    )

    val service: com.lightform._2auth.scalaapi.interfaces.OAuth2Endpoints[Try] =
      new OAuth2Endpoints[Try](backend, backend, backend, codeService)
  }

  "the handleAuthorizationRequest method" should "recover oauth errors" in new fixtures {
    val response = service
      .handleAuthorizationRequest(
        testUserId,
        AuthorizationRequest("code", "boom", None, Set.empty, None)
      )
      .success
      .value
    response.left.value.getError.getValue shouldEqual "boom"
  }
}
