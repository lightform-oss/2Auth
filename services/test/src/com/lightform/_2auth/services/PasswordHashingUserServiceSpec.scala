package com.lightform._2auth.services

import com.lightform._2auth.services.PasswordHashingUserService.{
  UserMeta,
  UserRepository
}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.util.{Success, Try}
import cats.implicits._
import org.scalatest.{OptionValues, TryValues}

class PasswordHashingUserServiceSpec
    extends AnyFlatSpec
    with Matchers
    with TryValues
    with OptionValues {
  it should "work" in {
    val password = "password"
    var hash     = ""

    val svc = new PasswordHashingUserService[Try](new UserRepository[Try] {
      def retrieveUser(
          username: String
      ) = Success(Some(UserMeta("bob", hash)))
    })

    hash = svc.hash(password)

    svc.authenticateUser("any", password).success.value.value shouldEqual "bob"
  }
}
