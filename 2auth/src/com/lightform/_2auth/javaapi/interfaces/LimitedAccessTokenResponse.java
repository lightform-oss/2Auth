package com.lightform._2auth.javaapi.interfaces;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

public interface LimitedAccessTokenResponse {
	String getAccessToken();
	String getTokenType();
	Optional<Duration> getExpiresIn();
	Set<String> getScope();
	Optional<String> getState();
}
