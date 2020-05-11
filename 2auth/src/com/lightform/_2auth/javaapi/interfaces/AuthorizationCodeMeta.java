package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;
import java.util.Set;

public interface AuthorizationCodeMeta {
	String getUserId();
	String getClientId();
	Optional<String> getRedirectUri();
	Set<String> getScope();
}
