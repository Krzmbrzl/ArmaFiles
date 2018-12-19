package raven.preprocessor;

/**
 * An option that can be given to a {@linkplain Preprocessor} instance
 * indicating how errors concerning whitespace should be dealt with
 * 
 * @author Raven
 *
 */
public enum PreprocessorWhitespaceHandling {
	/**
	 * Don't tolerate WS errors -> bail out on encounter<br>
	 * This is the way Arma itself handles those errors so if you are trying to get
	 * the same result as with Arma you should use this option.
	 */
	STRICT,
	/**
	 * Don't freak out - it's just a misplaced whitespace - issue an error but keep
	 * going as if the WS hadn't been there.
	 */
	TOLERANT
}
