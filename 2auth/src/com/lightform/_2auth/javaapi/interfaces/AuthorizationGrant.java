package com.lightform._2auth.javaapi.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public abstract class AuthorizationGrant {
	private AuthorizationGrant(){}

	public static abstract class TokenGrant extends AuthorizationGrant {
		public abstract String getAccessToken();
		public abstract String getTokenType();
		public abstract Optional<Duration> getExpiresIn();
		public abstract Set<String> getScope();
	}

	public abstract static class CodeGrant extends AuthorizationGrant {
		public abstract String getCode();
	}
}
