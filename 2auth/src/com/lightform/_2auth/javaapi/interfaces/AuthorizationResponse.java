package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;

public interface AuthorizationResponse {
	String getRedirectUri();
	AuthorizationGrant getGrant();
	Optional<String> getState();
}
