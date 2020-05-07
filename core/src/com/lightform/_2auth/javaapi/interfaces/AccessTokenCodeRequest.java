package com.lightform._2auth.javaapi.interfaces;

public interface AccessTokenCodeRequest extends AccessTokenRequest {
	String getCode();
	String getRedirectUri();
}
