package com.lightform._2auth.javaapi.interfaces;

import java.util.Set;

public interface AccessTokenRefreshRequest extends AccessTokenRequest {
	default String getGrantType() {
		return "refresh_token";
	}
	String getRefreshToken();
	Set<String> getScope();
}
