package com.lightform._2auth.scalaapi.interfaces

trait UserRepository[F[_]] {

  /**
    *
    * @param username
    * @param password
    * @return the user's ID if a users exists with these credentials, otherwise None
    */
  def authenticateUser(username: String, password: String): F[Option[String]]
}
