package com.lightform._2auth.javaapi.interfaces;

import com.lightform._2auth.javaapi.interfaces.Error;
import java.util.Optional;

public interface ErrorResponse {
	Error getError();
	Optional<String> getErrorDescription();
	Optional<String> getErrorUri();
}
