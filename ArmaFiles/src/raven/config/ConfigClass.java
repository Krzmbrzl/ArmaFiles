package raven.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import raven.misc.ByteReader;
import raven.misc.TextReader;

public class ConfigClass {

	/**
	 * The identifying int (four byte) at the beginning of each rapified file
	 */
	public static final int RAP_IDENTIFIER = 1348563456;

	/**
	 * The name of the class this one inherits from
	 */
	protected String parentClass;
	/**
	 * The amount of entries in this class
	 */
	protected int entryCount;
	/**
	 * The entries in this class
	 */
	protected ConfigClassEntry[] entries;
	/**
	 * The name of this class
	 */
	protected String name;


	public ConfigClass(String className, String parentClass, ConfigClassEntry[] entries) {
		this.parentClass = parentClass;
		this.name = className;
		this.entries = entries;
		this.entryCount = entries.length;
	}

	public static ConfigClass fromRapifiedFile(ByteReader reader) throws IOException, RapificationException {
		if (reader.readInt32() != RAP_IDENTIFIER) {
			throw new RapificationException("The given input is not rapified!");
		}

		// The next two four byte pieces must be 0 and 8 respectively
		if (reader.readInt32() != 0 || reader.readInt32() != 8) {
			throw new RapificationException("The given input doesn't follow the conventional format!");
		}

		@SuppressWarnings("unused")
		int offsetToEnums = reader.readInt32();

		return fromRapified(null, reader);
	}


	protected static ConfigClass fromRapified(String className, ByteReader reader)
			throws IOException, RapificationException {
		String parentClass = reader.readString();

		int entryCount = reader.readCompressedInt();

		if (entryCount < 0) {
			throw new RapificationException("The read entry-count is negative -> Invalid format in source!");
		}

		ConfigClassEntry[] entries = new ConfigClassEntry[entryCount];

		for (int i = 0; i < entryCount; i++) {
			byte entryType = reader.readByte();

			switch (entryType) {
			case ConfigClassEntry.SUBCLASS:
				entries[i] = SubclassEntry.fromRapified(reader);
				break;
			case ConfigClassEntry.ASSIGNMENT:
				entries[i] = ValueEntry.fromRapified(reader, false);
				break;
			case ConfigClassEntry.ARRAY:
				entries[i] = ArrayEntry.fromRapified(reader, false);
				break;
			case ConfigClassEntry.EXTERN:
			case ConfigClassEntry.DELETE:
				// ignore
				break;
			default:
				throw new RapificationException("Unknown entry type: " + entryType);
			}
		}

		Arrays.sort(entries, new Comparator<ConfigClassEntry>() {

			@Override
			public int compare(ConfigClassEntry o1, ConfigClassEntry o2) {
				if (o1 instanceof SubclassEntry && o2 instanceof SubclassEntry) {
					return ((SubclassEntry) o1).getOffsetToClassBody() - ((SubclassEntry) o2).getOffsetToClassBody();
				}
				if (o1 instanceof SubclassEntry) {
					return -1;
				} else {
					if (o2 instanceof SubclassEntry) {
						return 1;
					} else {
						return 0;
					}
				}
			}
		});

		boolean foundSubclass = false;

		for (ConfigClassEntry entry : entries) {
			if (entry instanceof SubclassEntry) {
				foundSubclass = true;

				((SubclassEntry) entry).processClass(reader);
			} else {
				if (foundSubclass) {
					throw new RapificationException(
							"Found Subclass- and non-Subclass-Entries contained in the same class -> Bad format");
				}
			}
		}

		return new ConfigClass(null, parentClass, entries);
	}

	public static ConfigClass fromTextFile(TextReader reader) throws IOException, ConfigException {
		return fromText(reader, null, "");
	}

	protected static ConfigClass fromText(TextReader reader, String className, String parentClass)
			throws IOException, ConfigException {
		List<ConfigClassEntry> entries = new ArrayList<>();

		reader.consumeWhithespace();

		int c;
		while ((c = reader.peek()) != -1 && c != '}') {
			entries.add(ConfigClassEntry.fromText(reader));

			reader.consumeWhithespace();
			reader.expect(';');
			reader.consumeWhithespace();
		}

		return new ConfigClass(className, "", entries.toArray(new ConfigClassEntry[entries.size()]));
	}

	/**
	 * Gets the name of this class - May be <code>null</code> if this is the
	 * implicit file-class
	 */
	public String getName() {
		return name;
	}

	/**
	 * Checks if this class has a specified name
	 */
	public boolean hasName() {
		return getName() != null;
	}

	/**
	 * Sets the name of this config class
	 * 
	 * @param name
	 *            The new name for this class
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Gets the amount of entries in this class
	 */
	public int getEntryCount() {
		return entries.length;
	}

	/**
	 * Gets the entries of this class
	 */
	public ConfigClassEntry[] getEntries() {
		return Arrays.copyOf(entries, entries.length);
	}

	@Override
	public String toString() {
		return "ConfigClass " + (hasName() ? "\"" + getName() + "\" " : "") + "- " + getEntryCount() + " entries";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ConfigClass)) {
			return false;
		}

		ConfigClass other = (ConfigClass) o;

		return (this.parentClass == null ? other.parentClass == null : this.parentClass.equals(other.parentClass))
				&& this.entryCount == other.entryCount
				&& (this.name == null ? other.name == null : this.name.equals(other.name))
				&& Arrays.deepEquals(this.entries, other.entries);
	}

}
