package raven.misc;

/**
 * An interface describing an object that can be transformed into a
 * text-representation
 * 
 * @author Raven
 *
 */
public interface ITextifyable {

	/**
	 * Creates a text-representation of this file
	 */
	public String toText();
}
