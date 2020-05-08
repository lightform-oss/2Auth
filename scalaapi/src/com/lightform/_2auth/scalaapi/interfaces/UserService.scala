package com.lightform._2auth.scalaapi.interfaces

trait UserService[F[_]] {

  /**
    * @param username
    * @param password
    * @return the user's ID if a users exists with these credentials, otherwise None
    */
  def authenticateUser(username: String, password: String): F[Option[String]]
}
