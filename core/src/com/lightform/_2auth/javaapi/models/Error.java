package com.lightform._2auth.javaapi.models;

public enum Error implements com.lightform._2auth.javaapi.interfaces.Error {
	INVALID_REQUEST("invalid_request"), INVALID_CLIENT("invalid_client"),
	INVALID_GRANT("invalid_grant"), UNAUTHORIZED_CLIENT("unauthorized_client"),
	UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
	INVALID_SCOPE("invalid_scope"), 
	UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type");

	public final String value;

	Error(String value){
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
