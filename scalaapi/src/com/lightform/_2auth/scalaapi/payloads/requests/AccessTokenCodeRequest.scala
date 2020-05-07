package com.lightform._2auth.scalaapi.payloads.requests

import com.lightform._2auth.javaapi.interfaces.{
  AccessTokenCodeRequest => JCodeRequest
}

case class AccessTokenCodeRequest(
    grant_type: "authorization_code",
    code: String,
    redirect_uri: String
) extends JCodeRequest {
  val getGrantType   = grant_type
  def getCode        = code
  def getRedirectUri = redirect_uri
}
