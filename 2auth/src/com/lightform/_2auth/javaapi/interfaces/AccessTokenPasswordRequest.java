package com.lightform._2auth.javaapi.interfaces;

import java.util.Set;

public interface AccessTokenPasswordRequest extends AccessTokenRequest {
	default String getGrantType() {
		return "password";
	}

	String getUsername();
	String getPassword();
	Set<String> getScope();
}
