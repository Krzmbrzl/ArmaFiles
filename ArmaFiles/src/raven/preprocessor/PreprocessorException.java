package raven.preprocessor;

public class PreprocessorException extends Exception {

	private static final long serialVersionUID = 2340284679023008194L;

	public PreprocessorException(String message) {
		super(message);
	}

	public PreprocessorException(Throwable cause) {
		super(cause);
	}

	public PreprocessorException(String message, Throwable cause) {
		super(message, cause);
	}

	public PreprocessorException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
