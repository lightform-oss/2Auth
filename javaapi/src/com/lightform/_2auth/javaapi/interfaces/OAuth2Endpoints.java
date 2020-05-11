package com.lightform._2auth.javaapi.interfaces;

import com.lightform._2auth.services.JwtAuthorizationCodeService;
import scala.Option;
import scala.util.Either;

import scala.jdk.javaapi.OptionConverters;

import com.lightform._2auth.scalaapi.interfaces.UserService;
import com.lightform._2auth.scalaapi.interfaces.TokenService;
import com.lightform._2auth.scalaapi.interfaces.ClientService;
import com.lightform._2auth.scalaapi.interfaces.AuthorizationCodeService;
import com.lightform._2auth.scalaapi.CompletionStageMonadError;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

public class OAuth2Endpoints implements com.lightform._2auth.scalaapi.interfaces.OAuth2Endpoints<CompletionStage> {

	private com.lightform._2auth.scalaapi.interfaces.OAuth2Endpoints<CompletionStage> underlying = null;

	public OAuth2Endpoints(UserService<CompletionStage> userService, TokenService<CompletionStage> tokenService,
			ClientService<CompletionStage> clientService, AuthorizationCodeService<CompletionStage> codeService) {
		underlying = new com.lightform._2auth.scalaapi.OAuth2Endpoints(userService, tokenService, clientService,
				codeService, CompletionStageMonadError.instance());
	}

	public CompletionStage<Either<ErrorResponse, Either<LimitedAccessTokenResponse, AuthorizationResponse>>> handleAuthorizationRequest(
			String userId, AuthorizationRequest request) {
		return underlying.handleAuthorizationRequest(userId, request);
	}

	public CompletionStage<Either<ErrorResponse, AccessTokenResponse>> handleTokenRequest(AccessTokenRequest request,
			Optional<String> clientId, Optional<String> clientSecret) {
		return underlying.handleTokenRequest(request, OptionConverters.toScala(clientId),
				OptionConverters.toScala(clientSecret));
	}

	@Override
	public CompletionStage<Either<ErrorResponse, AccessTokenResponse>> handleTokenRequest(AccessTokenRequest request,
			Option<String> clientId, Option<String> clientSecret) {
		return underlying.handleTokenRequest(request, clientId, clientSecret);
	}
}
