package com.lightform._2auth.scalaapi.payloads.requests

import com.lightform._2auth.javaapi.interfaces
import scala.jdk.OptionConverters._

case class AccessTokenCodeRequest(
    grant_type: "authorization_code",
    code: String,
    redirect_uri: Option[String]
) extends interfaces.AccessTokenCodeRequest {
  val getGrantType   = grant_type
  def getCode        = code
  def getRedirectUri = redirect_uri.toJava
}
