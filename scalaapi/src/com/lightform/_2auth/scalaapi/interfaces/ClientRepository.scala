package com.lightform._2auth.scalaapi.interfaces

trait ClientRepository[F[_]] {

  /**
    *
    * @param clientId
    * @param clientSecret
    * @return true if the clientSecret is valid for the given clientId
    */
  def validateClient(clientId: String, clientSecret: String): F[Boolean]
}
