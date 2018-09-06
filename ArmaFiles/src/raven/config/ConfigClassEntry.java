package raven.config;

import java.io.IOException;

import raven.misc.ITextifyable;
import raven.misc.TextReader;

/**
 * A general representation of an entry inside a {@linkplain ConfigClass}
 * 
 * @author Raven
 *
 */
public abstract class ConfigClassEntry implements ITextifyable {

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

	/**
	 * Creates an arbitrary {@linkplain ConfigClassEntry} from a text config file.
	 * It will determine the sub-type to be created on its own. This method assumes
	 * that the reader points directly at an entry specification. Note that this
	 * method may not be used for nested entries as occurring inside arrays.
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created entry
	 * @throws IOException
	 * @throws ConfigException
	 */
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
			
			if(reader.peek() == ';') {
				return new SubclassEntry(new ConfigClass(name, "", new ConfigClassEntry[0]));
			} else {
			return SubclassEntry.fromText(reader, name);
			}
		}
	}

}
