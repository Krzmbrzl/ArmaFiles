package raven.config;

/**
 * A class representing a field entry inside a config class. A field is a
 * variable that is being assigned in the config class or an implicit field
 * without an variable name (e.g. in nested arrays)
 * 
 * @author Raven
 *
 */
public abstract class FieldEntry extends ConfigClassEntry {

	/**
	 * The name of the variable the array is being assigned to
	 */
	protected String varName;

	public FieldEntry(String varName) {
		this.varName = varName;
	}

	/**
	 * Gets the variable name of this field
	 */
	public String getVarName() {
		return varName;
	}

	/**
	 * Checks whether this field has a variable name
	 */
	public boolean hasVarName() {
		return varName != null;
	}
	
	@Override
	public String toText() {
		return (hasVarName() ? getVarName() + " = " : "") + getFieldValueString();
	}

	/**
	 * Gets the value of this field as a String representation
	 */
	public abstract String getFieldValueString();

}
