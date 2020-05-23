package org.likide.bootstrap;

public class DownloadFailureException extends Exception {

	private static final long serialVersionUID = -3381307371805623353L;

	public DownloadFailureException(String message) {
		super(message);
	}

	public DownloadFailureException(String message, Throwable cause) {
		super(message, cause);
	}

}
