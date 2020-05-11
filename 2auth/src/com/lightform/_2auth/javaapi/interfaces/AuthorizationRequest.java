package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;
import java.util.Set;

public interface AuthorizationRequest {
	String getResponseType();
	String getClientId();
	Optional<String> getRedirectUri();
	Set<String> getScope();
	Optional<String> getState();
}
