package raven.preprocessor;

/**
 * An option given to the {@linkplain Preprocessor} determining whether the bugs
 * present in the Arma-preprocessor should be reproduced
 * 
 * @author Raven
 *
 */
public enum PreprocessorBugReproduction {
	/**
	 * Don't reproduce bugs
	 */
	NONE,
	/**
	 * Reproduce the bugs of the Arma-preprocessor
	 */
	ARMA
}
