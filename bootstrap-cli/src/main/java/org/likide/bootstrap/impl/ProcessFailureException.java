package org.likide.bootstrap.impl;

public class ProcessFailureException extends Exception {

	private static final long serialVersionUID = -3381307371805623353L;

	public ProcessFailureException(String message) {
		super(message);
	}

	public ProcessFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
