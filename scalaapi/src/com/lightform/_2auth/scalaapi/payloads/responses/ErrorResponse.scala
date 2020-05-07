package com.lightform._2auth.scalaapi.payloads.responses

import com.lightform._2auth.javaapi.interfaces
import com.lightform._2auth.javaapi.interfaces.Error
import scala.jdk.OptionConverters._

case class ErrorResponse(
    error: Error,
    error_description: Option[String],
    error_uri: Option[String] = None
) extends Exception(
      s"${error.getValue}: ${error_description.getOrElse("")} ${error_uri.getOrElse("")}"
    )
    with interfaces.ErrorResponse {
  def getError            = error
  def getErrorDescription = error_description.toJava
  def getErrorUri         = error_uri.toJava
}
