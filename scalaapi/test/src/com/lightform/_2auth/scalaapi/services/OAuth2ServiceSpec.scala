package com.lightform._2auth.scalaapi.services

import cats.{Id, Monad, MonadError}
import com.lightform._2auth.scalaapi.payloads.responses.ErrorResponse
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import cats.implicits._

import scala.util.Try

class OAuth2ServiceSpec extends AnyFlatSpec with Matchers {
  "Password grants" should "reject non-existent users" in new fixtures {}

  trait fixtures {

    val service: com.lightform._2auth.scalaapi.interfaces.OAuth2Service[Try] =
      new OAuth2Service[Try]()
  }
}
