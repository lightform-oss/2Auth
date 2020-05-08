package com.lightform._2auth.scalaapi.interfaces

trait ClientService[F[_]] {

  /**
    * @param clientId
    * @return Some if the client has a registered redirect uri
    */
  def retrieveClientRedirectUri(clientId: String): F[Option[String]]

  /**
    * @param clientId
    * @param clientSecret
    * @return true if the clientSecret is valid for the given clientId
    */
  def validateClient(clientId: String, clientSecret: String): F[Boolean]
}
