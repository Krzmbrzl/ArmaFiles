package raven.config;

import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

public class ArrayEntry extends ConfigClassEntry {

	/**
	 * The name of the variable the array is being assigned to
	 */
	protected String varName;
	/**
	 * The {@linkplain ArrayStruct} containing the array's content
	 */
	protected ArrayStruct content;
	/**
	 * Indicating whether this is a nested array-entry
	 */
	protected boolean nested;


	public ArrayEntry(String varName, ArrayStruct content) {
		this.varName = varName;
		this.content = content;
		this.nested = varName == null;
	}

	@Override
	public byte getType() {
		return ConfigClassEntry.ARRAY;
	}


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
	 * Gets the variable name of the variable this entry is being assigned to. May
	 * be <code>null</code> for nested array-entries
	 */
	public String getVarName() {
		return varName;
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

}
