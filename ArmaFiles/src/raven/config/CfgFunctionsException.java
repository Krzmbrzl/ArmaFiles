package raven.config;

public class CfgFunctionsException extends ConfigException {

	private static final long serialVersionUID = -8208881497549380670L;

	public CfgFunctionsException() {
	}

	public CfgFunctionsException(String message) {
		super(message);
	}

	public CfgFunctionsException(Throwable cause) {
		super(cause);
	}

	public CfgFunctionsException(String message, Throwable cause) {
		super(message, cause);
	}

	public CfgFunctionsException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
