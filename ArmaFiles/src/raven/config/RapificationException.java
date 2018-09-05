package raven.config;

public class RapificationException extends Exception {

	private static final long serialVersionUID = -8229553091528581075L;

	public RapificationException() {
	}

	public RapificationException(String message) {
		super(message);
	}

	public RapificationException(Throwable cause) {
		super(cause);
	}

	public RapificationException(String message, Throwable cause) {
		super(message, cause);
	}

	public RapificationException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
