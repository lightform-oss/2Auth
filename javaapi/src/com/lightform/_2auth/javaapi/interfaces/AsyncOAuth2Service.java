package com.lightform._2auth.javaapi.interfaces;

import com.lightform._2auth.scalaapi.interfaces.OAuth2Service;
import scala.Option;
import scala.util.Either;

import java.util.concurrent.CompletionStage;

public class AsyncOAuth2Service implements OAuth2Service<CompletionStage> {

	@Override
	public CompletionStage<Either<ErrorResponse, AccessTokenResponse>> handleTokenRequest(AccessTokenRequest request, Option<String> clientId, Option<String> clientSecret) {
		return null;
	}
}
