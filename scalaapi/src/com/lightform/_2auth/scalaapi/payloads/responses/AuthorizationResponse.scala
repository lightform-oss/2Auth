package com.lightform._2auth.scalaapi.payloads.responses

import com.lightform._2auth.javaapi.interfaces
import java.{util => ju}
import scala.jdk.OptionConverters._

final case class AuthorizationResponse(code: String, state: Option[String])
    extends interfaces.AuthorizationResponse {

  def getCode  = code
  def getState = state.toJava

}
