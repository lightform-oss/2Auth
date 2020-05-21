package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;

public interface AuthorizationErrorResponse {
	ErrorResponse getError();
	Optional<String> getState();
	Optional<String> getRedirectUri();
	boolean isInQuery();
	boolean isInFragment();
}
