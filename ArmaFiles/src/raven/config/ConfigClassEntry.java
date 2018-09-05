package raven.config;

import java.io.IOException;

import raven.misc.TextReader;

public abstract class ConfigClassEntry {

	/**
	 * The entry type indicating a nested sub-class
	 */
	public static final byte SUBCLASS = 0;
	/**
	 * The entry type indicating an value-assignment
	 */
	public static final byte ASSIGNMENT = 1;
	/**
	 * The entry type indicating an array-assignment
	 */
	public static final byte ARRAY = 2;
	/**
	 * The entry type indicating an extern class definition
	 */
	public static final byte EXTERN = 3;
	/**
	 * The entry type indicating a delete-statement
	 */
	public static final byte DELETE = 4;

	/**
	 * Gets the type of this entry
	 */
	public abstract byte getType();

	@Override
	public String toString() {
		return "ConfigClassEntry - type: " + getType();
	}

	protected static ConfigClassEntry fromText(TextReader reader) throws IOException, ConfigException {
		reader.consumeWhithespace();

		String id = reader.readWord();
		boolean isArray = false;
		if (reader.peek() == '[') {
			reader.expect('[');
			reader.expect(']');
			isArray = true;
		}

		reader.consumeWhithespace();

		if (reader.peek() == (int) '=') {
			// it is an assignment -> Either ArrayEntry or ValueEntry
			// consume =
			reader.read();
			reader.consumeWhithespace();

			if (isArray) {
				// it's an array
				return ArrayEntry.fromText(reader, id);
			} else {
				// it's a value
				return ValueEntry.fromText(reader, id);
			}
		} else {
			// must be a class-definition
			if (!id.equals("class")) {
				throw new ConfigException("Expected class definition!");
			}
			
			String name = reader.readWord();
			reader.consumeWhithespace();

			return SubclassEntry.fromText(reader, name);
		}
	}

}
