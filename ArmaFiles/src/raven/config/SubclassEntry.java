package raven.config;

import java.io.IOException;

import raven.misc.ByteReader;
import raven.misc.TextReader;

public class SubclassEntry extends ConfigClassEntry {

	/**
	 * The offset to the class's body
	 */
	protected int offsetToBody;
	/**
	 * The name of the referenced class
	 */
	protected String className;
	/**
	 * The {@linkplain ConfigClass} referenced by this entry
	 */
	protected ConfigClass referencedClass;

	public SubclassEntry(ConfigClass referencedClass) {
		this(referencedClass.getName(), -1);
		this.referencedClass = referencedClass;
	}

	public SubclassEntry(String className, int offsetToClassBody) {
		this.className = className;
		this.offsetToBody = offsetToClassBody;
	}

	@Override
	public byte getType() {
		return ConfigClassEntry.SUBCLASS;
	}

	protected static SubclassEntry fromRapified(ByteReader reader) throws IOException, RapificationException {
		String className = reader.readString();

		if (className.isEmpty()) {
			throw new RapificationException("Empty class name in entry!");
		}

		int offsetToBody = reader.readInt32();

		return new SubclassEntry(className, offsetToBody);
	}

	protected static SubclassEntry fromText(TextReader reader, String className) throws IOException, ConfigException {
		String parentClass = "";

		if (reader.peek() == ':') {
			// read parent class
			reader.expect(':');
			reader.consumeWhithespace();

			parentClass = reader.readWord();

			reader.consumeWhithespace();
		}

		reader.expect('{');

		ConfigClass subClass = ConfigClass.fromText(reader, className, parentClass);

		reader.expect('}');

		return new SubclassEntry(subClass);
	}

	/**
	 * Gets the name of the referenced class
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * Gets the {@linkplain ConfigClass} referenced by this entry.
	 */
	public ConfigClass getReferncedClass() {
		return referencedClass;
	}

	/**
	 * Gets the offset of the class-content's start inside the rapified file
	 */
	public int getOffsetToClassBody() {
		return offsetToBody;
	}

	/**
	 * Processes the class referenced by this entry by creating it off the given
	 * reader's input. The reader will be advanced to the respective offset.
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @throws IOException
	 * @throws RapificationException
	 * @throws IllegalArgumentException
	 *             If the given reader's position is beyond
	 *             {@link #getOffsetToClassBody()}
	 */
	public void processClass(ByteReader reader) throws IOException, RapificationException {
		if (reader.getPosition() > getOffsetToClassBody()) {
			throw new IllegalArgumentException("The provided reader has advanced over the necessary content!");
		}

		// advance reader to respective position
		while (reader.getPosition() < getOffsetToClassBody()) {
			reader.read();
		}

		referencedClass = ConfigClass.fromRapified(getClassName(), reader);
	}

	@Override
	public String toString() {
		return "SubclassEntry: \"" + getClassName() + "\"";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SubclassEntry)) {
			return false;
		}

		SubclassEntry other = (SubclassEntry) o;

		return this.offsetToBody == other.offsetToBody
				&& (this.className == null ? other.className == null : this.className.equals(other.className))
				&& (this.referencedClass == null ? other.referencedClass == null
						: this.referencedClass.equals(other.referencedClass));
	}

}
