package raven.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import raven.misc.ByteReader;
import raven.misc.ITextifyable;
import raven.misc.TextReader;

/**
 * A class representing an Arma-config-class
 * 
 * @author Raven
 *
 */
public class ConfigClass implements ITextifyable {

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


	/**
	 * Creates a new instance of this class
	 * 
	 * @param className
	 *            The name of the represented config class or <code>null</code> if
	 *            there is none
	 * @param parentClass
	 *            The name of the class the represented one inherits from or an
	 *            empty String if there is none
	 * @param entries
	 *            An array of entries directly specified inside the represented
	 *            config class
	 */
	public ConfigClass(String className, String parentClass, ConfigClassEntry[] entries) {
		assert (parentClass != null);
		assert (entries != null);

		this.parentClass = parentClass;
		this.name = className;
		this.entries = entries;
		this.entryCount = entries.length;
	}

	/**
	 * Creates a {@linkplain ConfigClass} from a rapified file. This method assumes
	 * the given reader hasn't consumed any data yet and validates the presence of
	 * the initial {@link #RAP_IDENTIFIER} bytes.
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created class
	 * @throws IOException
	 * @throws RapificationException
	 */
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

	/**
	 * Creates a {@linkplain ConfigClass} from a rapified file. This method assumes
	 * the given reader points directly at the start of a class-definition.
	 * 
	 * @param className
	 *            The name of the class to be created
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created class
	 * @throws IOException
	 * @throws RapificationException
	 */
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
			boolean plusEqual = false;

			switch (entryType) {
			case ConfigClassEntry.SUBCLASS:
				entries[i] = SubclassEntry.fromRapified(reader);
				break;
			case ConfigClassEntry.ASSIGNMENT:
				entries[i] = ValueEntry.fromRapified(reader, false);
				break;
			case ConfigClassEntry.PLUSEQUAL_ARRAY:
				// skip the next four bytes
				reader.skip(4);
				plusEqual = true;
			case ConfigClassEntry.ARRAY:
				entries[i] = ArrayEntry.fromRapified(reader, false, plusEqual);
				break;
			case ConfigClassEntry.EXTERN:
				entries[i] = new SubclassEntry(new ConfigClass(reader.readString(), "", new ConfigClassEntry[0]));
				break;
			case ConfigClassEntry.DELETE:
				// ignore class-deleting for now -> Read name and discard
				reader.readString();
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

		for (ConfigClassEntry entry : entries) {
			if (entry instanceof SubclassEntry) {
				((SubclassEntry) entry).processClass(reader);
			}
		}

		return new ConfigClass(className, parentClass, entries);
	}

	/**
	 * Creates a {@linkplain ConfigClass} from a text config file. This method
	 * assumes that the given reader has not yet consumed anything from the input
	 * stream
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created class
	 * @throws IOException
	 * @throws ConfigException
	 */
	public static ConfigClass fromTextFile(TextReader reader) throws IOException, ConfigException {
		return fromText(reader, null, "");
	}

	/**
	 * Creates a {@linkplain ConfigClass} from a text config file. This method
	 * assumes that the given reader has not yet consumed anything from the input
	 * stream
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @param className
	 *            The name of the class to be created or <code>null</code> if there
	 *            is none
	 * @param parentClass
	 *            The name of the class the one to be created inherits from or an
	 *            empty String if there is none
	 * @return The created class
	 * @throws IOException
	 * @throws ConfigException
	 */
	protected static ConfigClass fromText(TextReader reader, String className, String parentClass)
			throws IOException, ConfigException {
		List<ConfigClassEntry> entries = new ArrayList<>();

		reader.consumeWhitespace();

		int c;
		while ((c = reader.peek()) != -1 && c != '}') {
			entries.add(ConfigClassEntry.fromText(reader));

			reader.consumeWhitespace();
			reader.expect(';');
			reader.consumeWhitespace();
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
	 * Gets this class's parent class or an empty String if there is none
	 */
	public String getParentClass() {
		return parentClass;
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

	/**
	 * Gets the {@linkplain ConfigClassEntry} with the specified index
	 * 
	 * @param index
	 *            The index of the entry to retrieve
	 * @return The respective entry
	 */
	public ConfigClassEntry getEntry(int index) {
		return entries[index];
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

	/**
	 * Tries to find an assignment field of the given name in this class. If the
	 * recursive flag is being set, it will pass the request recursively to all its
	 * subclasses.
	 * 
	 * @param name
	 *            The name of the field to search for
	 * @param recursive
	 *            Whether to do a recursive search for the field
	 * @return The respective field or <code>null</code> if none could be found
	 */
	public FieldEntry getField(String name, boolean recursive) {
		name = name.toLowerCase();
		for (ConfigClassEntry current : getEntries()) {
			if (current instanceof ValueEntry) {
				ValueEntry valEntry = (ValueEntry) current;

				if (valEntry.hasVarName() && valEntry.getVarName().toLowerCase().equals(name)) {
					return valEntry;
				}
			} else {
				if (current instanceof ArrayEntry) {
					ArrayEntry arrEntry = (ArrayEntry) current;

					if (arrEntry.hasVarName() && arrEntry.getVarName().equals(name)) {
						return arrEntry;
					}
				} else {
					if (recursive && current instanceof SubclassEntry && !((SubclassEntry) current).isExtern()) {
						FieldEntry entry = ((SubclassEntry) current).getReferencedClass().getField(name, recursive);

						if (entry != null) {
							return entry;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Tries to find a subclass with the given name. If the recursive flag is set,
	 * the search is passed consecutively to all subclasses.
	 * 
	 * @param name
	 *            The name of the class to search for
	 * @param recursive
	 *            Whether to do a recursive search for the class
	 * @return The respective {@linkplain ConfigClass} or <code>null</code> if none
	 *         could be found
	 */
	public ConfigClass getSubclass(String name, boolean recursive) {
		name = name.toLowerCase();
		
		for (ConfigClassEntry current : getEntries()) {
			if (current instanceof SubclassEntry && !((SubclassEntry) current).isExtern()) {
				if (((SubclassEntry) current).getClassName().toLowerCase().equals(name)) {
					return ((SubclassEntry) current).getReferencedClass();
				} else {
					if (recursive) {
						ConfigClass result = ((SubclassEntry) current).getReferencedClass().getSubclass(name,
								recursive);

						if (result != null) {
							return result;
						}
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks how many fields are being directly defined inside this config class
	 */
	public int getFieldCount() {
		int amount = 0;

		for (ConfigClassEntry current : entries) {
			if (current instanceof FieldEntry) {
				amount++;
			}
		}

		return amount;
	}

	/**
	 * Checks how many subclasses are being directly defined inside this config
	 * class
	 */
	public int getSubclassCount() {
		return getEntryCount() - getFieldCount();
	}

	/**
	 * Gets all {@linkplain FieldEntry}s directly defined in this config class
	 */
	public FieldEntry[] getFields() {
		FieldEntry[] entries = new FieldEntry[getFieldCount()];

		int counter = 0;

		for (ConfigClassEntry current : this.entries) {
			if (current instanceof FieldEntry) {
				entries[counter] = (FieldEntry) current;
				counter++;
			}
		}

		return entries;
	}

	/**
	 * Gets all {@linkplain SubclassEntry}s directly defined in this config class
	 */
	public SubclassEntry[] getSubclasses() {
		SubclassEntry[] entries = new SubclassEntry[getSubclassCount()];

		int counter = 0;

		for (ConfigClassEntry current : this.entries) {
			if (current instanceof SubclassEntry) {
				entries[counter] = (SubclassEntry) current;
				counter++;
			}
		}

		return entries;
	}

	@Override
	public String toText() {
		StringBuilder builder = new StringBuilder();

		builder.append("class " + getName());
		if (!getParentClass().isEmpty()) {
			builder.append(" : " + getParentClass());
		}

		builder.append(" {\n\t");

		for (ConfigClassEntry currentEntry : entries) {
			builder.append("\t" + currentEntry.toText().replace("\n", "\n\t"));

			if (!(currentEntry instanceof SubclassEntry)) {
				builder.append(";");
			}
		}

		builder.append("};");

		return builder.toString();
	}
}
