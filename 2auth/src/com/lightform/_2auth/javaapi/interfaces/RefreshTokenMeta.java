package com.lightform._2auth.javaapi.interfaces;

import java.util.Optional;
import java.util.Set;

public interface RefreshTokenMeta {
	Optional<String> getUserId();
	Optional<String> getClientId();
	boolean isConfidentialClient();
	Set<String> getScope();
}
