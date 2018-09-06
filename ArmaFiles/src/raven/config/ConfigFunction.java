package raven.config;

/**
 * A class representing a wrapper for all relevant information on a function
 * defined inside a CfgFunctions class
 * 
 * @author Raven
 *
 */
public class ConfigFunction {
	/**
	 * The name of the function
	 */
	public String name;
	/**
	 * The path to the function (from mission or game root)
	 */
	public String path;
	/**
	 * The specified attributes for this function
	 */
	public String[] attributes;

	/**
	 * Creates a new instance of this object
	 * 
	 * @param name
	 *            The name of this function
	 * @param path
	 *            The path to the function (from mission or game root)
	 * @param attributes
	 *            The specified attributes for this function
	 */
	public ConfigFunction(String name, String path, String[] attributes) {
		this.name = name;
		this.path = path;
		this.attributes = attributes;
	}

}
