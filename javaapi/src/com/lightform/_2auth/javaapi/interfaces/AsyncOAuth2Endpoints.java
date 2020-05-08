package com.lightform._2auth.javaapi.interfaces;

import com.lightform._2auth.scalaapi.interfaces.OAuth2Endpoints;
import scala.Option;
import scala.util.Either;

import java.util.concurrent.CompletionStage;

public class AsyncOAuth2Endpoints implements OAuth2Endpoints<CompletionStage> {

	@Override
	public CompletionStage<Either<ErrorResponse, Either<LimitedAccessTokenResponse, AuthorizationResponse>>> handleAuthorizationRequest(String userId, AuthorizationRequest request) {
		return null;
	}

	@Override
	public CompletionStage<Either<ErrorResponse, AccessTokenResponse>> handleTokenRequest(AccessTokenRequest request, Option<String> clientId, Option<String> clientSecret) {
		return null;
	}
}
