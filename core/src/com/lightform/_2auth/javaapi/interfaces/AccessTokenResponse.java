package com.lightform._2auth.javaapi.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface AccessTokenResponse {
	String getAccessToken();
	String getTokenType();
	Optional<Duration> getExpiresIn();
	Optional<String> getRefreshToken();
	Set<String> getScope();
}
