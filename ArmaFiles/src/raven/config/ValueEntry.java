package raven.config;

import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

public class ValueEntry extends ConfigClassEntry {

	/**
	 * Indicator for a String-type
	 */
	public static final byte STRING = 0;
	/**
	 * Indicator for a float-type
	 */
	public static final byte FLOAT = 1;
	/**
	 * Indicator for a long-type
	 */
	public static final byte LONG = 2;


	/**
	 * The data type of this value
	 */
	protected byte dataType;
	/**
	 * The name of the variable assigned a value by this entry (if any)
	 */
	protected String varName;
	/**
	 * The String value of the value if it is of type {@link #STRING}
	 */
	protected String string;
	/**
	 * The float value of the value if it is of type {@link #FLOAT}
	 */
	protected float fl;
	/**
	 * The long value of this value if it is of type {@link #LONG}
	 */
	protected long lo;
	/**
	 * Indicating whether this is a nested entry (e.g. in an array)
	 */
	protected boolean nested;


	public ValueEntry(String varName, String content) {
		this.varName = varName;
		this.nested = varName == null;
		this.string = content;
		this.dataType = STRING;
	}

	public ValueEntry(String varName, float content) {
		this.varName = varName;
		this.nested = varName == null;
		this.fl = content;
		this.dataType = FLOAT;
	}

	public ValueEntry(String varName, long content) {
		this.varName = varName;
		this.nested = varName == null;
		this.lo = content;
		this.dataType = LONG;
	}

	@Override
	public byte getType() {
		return ConfigClassEntry.ASSIGNMENT;
	}

	protected static ValueEntry fromRapified(ByteReader reader, boolean isNested)
			throws IOException, RapificationException {
		byte dataType = reader.readByte();

		String varName = null;

		if (!isNested) {
			varName = reader.readString();

			if (varName.isEmpty()) {
				throw new RapificationException("Empty variable name!");
			}
		}

		switch (dataType) {
		case STRING:
			return new ValueEntry(varName, reader.readString());
		case FLOAT:
			return new ValueEntry(varName, reader.readFloat());
		case LONG:
			return new ValueEntry(varName, reader.readInt32());
		default:
			throw new RapificationException("Unknown data type: " + dataType);
		}
	}

	protected static ValueEntry fromText(TextReader reader, String varName) throws IOException {
		int c = reader.peek();

		if (c == '"' || c == '\'') {
			// String
			return new ValueEntry(varName, reader.readString());
		}
		if (Character.isDigit(c)) {
			float num = reader.readNumber();

			if (num == (long) num) {
				// must be a long
				return new ValueEntry(varName, (long) num);
			} else {
				// is indeed a float
				return new ValueEntry(varName, num);
			}
		}

		throw new IllegalStateException("Attempt to parse ValueEntry failed!");
	}

	/**
	 * Gets the data type of this assignment
	 */
	public byte getDataType() {
		return dataType;
	}

	/**
	 * Checks whether the given type matches the type of this entry
	 * 
	 * @param requiredType
	 *            The required type in order to access whatever method is calling
	 *            this one
	 */
	protected void checkAccess(byte requiredType) {
		if (requiredType != getDataType()) {
			throw new IllegalAccessError("Can't access method - required data type doesn't match!");
		}
	}

	/**
	 * Gets the string value that is being assigned. This method may only be called
	 * if {@link #getDataType()} returns {@link #STRING}
	 */
	public String getString() {
		checkAccess(STRING);
		return string;
	}

	/**
	 * Gets the float value that is being assigned. This method may only be called
	 * if {@link #getDataType()} returns {@link #FLOAT}
	 */
	public float getFloat() {
		checkAccess(FLOAT);
		return fl;
	}

	/**
	 * Gets the long value that is being assigned. This method may only be called if
	 * {@link #getDataType()} returns {@link #LONG}
	 */
	public long getLong() {
		checkAccess(LONG);
		return lo;
	}

	/**
	 * Checks whether this entry is a nested one (inside another array) that does
	 * not have a {@link #varName}
	 */
	public boolean isNested() {
		return nested;
	}

	/**
	 * Gets the variable name of the variable this entry is being assigned to. May
	 * be <code>null</code> if {@link #isNested()} returns <code>true</code>
	 */
	public String getVarName() {
		return varName;
	}

	@Override
	public String toString() {
		String str = "ValueEntry" + (isNested() ? "" : ": \"" + getVarName() + "\"") + " - ";

		switch (getDataType()) {
		case STRING:
			str += "String: \"" + getString() + "\"";
			break;
		case FLOAT:
			str += "Float: " + getFloat();
			break;
		case LONG:
			str += "LONG " + getFloat();
			break;
		default:
			str += "type=" + getType();
		}

		return str;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ValueEntry)) {
			return false;
		}

		ValueEntry other = (ValueEntry) o;

		return this.nested && (this.varName == null ? other.varName == null : this.varName.equals(other.varName))
				&& (this.string == null ? other.string == null : this.string.equals(other.string))
				&& this.fl == other.fl && this.lo == other.lo;
	}

}
