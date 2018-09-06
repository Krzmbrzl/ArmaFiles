package raven.config;

import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

/**
 * A class representing an array entry inside a {@linkplain ConfigClass} or an
 * array structure nested inside another array
 * 
 * @author Raven
 *
 */
public class ArrayEntry extends FieldEntry {
	/**
	 * The {@linkplain ArrayStruct} containing the array's content
	 */
	protected ArrayStruct content;
	/**
	 * Indicating whether this is a nested array-entry
	 */
	protected boolean nested;


	/**
	 * Creates a new instance if this class
	 * 
	 * @param varName
	 *            The variable name this array is being assigned to or
	 *            <code>null</code> if this is a nested array
	 * @param content
	 *            The {@linkplain ArrayStruct} representing this array's content
	 */
	public ArrayEntry(String varName, ArrayStruct content) {
		super(varName);
		this.content = content;
		this.nested = varName == null;
	}

	@Override
	public byte getType() {
		return ConfigClassEntry.ARRAY;
	}

	/**
	 * Creates an {@linkplain ArrayEntry} from a rapified config file. This method
	 * assumes that the given reader is right at the beginning of an array
	 * specification
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @param isNested
	 *            Whether the array to be read is nested inside another array (i.e.
	 *            does not have a variable name)
	 * @return The created entry
	 * @throws IOException
	 * @throws RapificationException
	 */
	protected static ArrayEntry fromRapified(ByteReader reader, boolean isNested)
			throws IOException, RapificationException {
		String varName = null;

		if (!isNested) {
			varName = reader.readString();

			if (varName.isEmpty()) {
				throw new RapificationException("Empty variable name!");
			}
		}

		ArrayStruct content = ArrayStruct.fromRapified(reader);

		return new ArrayEntry(varName, content);
	}

	/**
	 * Creates a new {@linkplain ArrayEntry} from a text config file.This method
	 * assumes that the given reader is right at the beginning of an array
	 * specification
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @param varName
	 *            The variable name this array is being assigned to or
	 *            <code>null</code> if this is a nested array
	 * @return The created entry
	 * @throws IOException
	 */
	protected static ArrayEntry fromText(TextReader reader, String varName) throws IOException {
		reader.consumeWhithespace();

		return new ArrayEntry(varName, ArrayStruct.fromText(reader));
	}

	/**
	 * Checks whether this entry is a nested one (inside another array) that does
	 * not have a {@link #varName}
	 */
	public boolean isNested() {
		return nested;
	}

	/**
	 * Gets the length of the array that is being represented by this entry
	 */
	public int length() {
		return content.length();
	}

	/**
	 * Gets the actual content of the represented array
	 */
	public ConfigClassEntry[] getContent() {
		return content.getContent();
	}

	@Override
	public String toString() {
		return "ArrayEntry" + (isNested() ? "" : ": \"" + getVarName() + "\"") + " - " + content.length() + " entries";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ArrayEntry)) {
			return false;
		}

		ArrayEntry other = (ArrayEntry) o;

		return (this.varName == null ? other.varName == null : this.varName.equals(other.varName))
				&& this.nested == other.nested && this.content.equals(other.content);
	}

	@Override
	public String getFieldValueString() {
		return content.toText();
	}

}
