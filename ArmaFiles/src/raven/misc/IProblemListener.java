package raven.misc;

public interface IProblemListener {

	/**
	 * Gets called whenever an error occurred
	 * 
	 * @param msg
	 *            The error message
	 * @param start
	 *            The start index of the error
	 * @param length
	 *            The length of the erroneous area
	 */
	public void error(String msg, int start, int length);

	/**
	 * Gets called whenever a warning is being produced
	 * 
	 * @param msg
	 *            The warning message
	 * @param start
	 *            The start index of the area affected by the warning
	 * @param length
	 *            The length of the affected area
	 */
	public void warning(String msg, int start, int length);
}
