package raven.preprocessor;

/**
 * An option given to the {@linkplain Preprocessor} indicating how comments
 * should be dealt with
 * 
 * @author Raven
 *
 */
public enum PreprocessorCommentHandling {
	/**
	 * Keep all comments in the code
	 */
	KEEP,
	/**
	 * Keep inline comments only
	 */
	KEEP_INLINE,
	/**
	 * Keep block-comments only
	 */
	KEEP_BLOCK,
	/**
	 * Remove all comments
	 */
	REMOVE
}
