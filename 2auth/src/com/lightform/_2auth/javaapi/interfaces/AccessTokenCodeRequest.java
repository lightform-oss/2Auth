package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;

public interface AccessTokenCodeRequest extends AccessTokenRequest {
	String getCode();
	Optional<String> getRedirectUri();
}
