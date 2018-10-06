package raven.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import raven.misc.ByteReader;
import raven.misc.ITextifyable;
import raven.misc.TextReader;

/**
 * A class representing the array-structure and therefore the actual
 * array-content
 * 
 * @author Raven
 *
 */
public class ArrayStruct implements ITextifyable {

	/**
	 * The subtype-identifier indicating a nested array
	 */
	public static final byte NESTED_ARRAY = 3;

	/**
	 * The content of this struct
	 */
	protected ConfigClassEntry[] content;
	/**
	 * The amount of elements in this struct
	 */
	protected int length;


	/**
	 * Creates a new instance of this class
	 * 
	 * @param content
	 *            The content that should be represented by this struct
	 */
	public ArrayStruct(ConfigClassEntry[] content) {
		this.content = content;
		this.length = content.length;
	}

	/**
	 * Creates an {@linkplain ArrayStruct} from a rapified config file. This method
	 * assumes that the given reader is right at the beginning of an array-struct
	 * specification
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created struct
	 * @throws IOException
	 * @throws RapificationException
	 */
	protected static ArrayStruct fromRapified(ByteReader reader) throws IOException, RapificationException {
		int length = reader.readCompressedInt();

		if (length < 0) {
			throw new RapificationException("Negative array length!");
		}

		ConfigClassEntry[] content = new ConfigClassEntry[length];

		for (int i = 0; i < length; i++) {
			byte type = reader.readByte();

			switch (type) {
			case ValueEntry.STRING:
			case ValueEntry.FLOAT:
			case ValueEntry.LONG:
				reader.unread(type);
				content[i] = ValueEntry.fromRapified(reader, true);
				break;
			case NESTED_ARRAY:
				content[i] = ArrayEntry.fromRapified(reader, true, false);
				break;
			default:
				throw new RapificationException("Unknown or unexpected data type in array struct: " + type);
			}
		}

		return new ArrayStruct(content);
	}

	/**
	 * Creates an {@linkplain ArrayStruct} from a text config file. This method
	 * assumes that the given reader is right at the beginning of an array-struct
	 * specification
	 * 
	 * @param reader
	 *            The reader to use as a data source
	 * @return The created struct
	 * @throws IOException
	 */
	protected static ArrayStruct fromText(TextReader reader) throws IOException {
		reader.expect('{');
		reader.consumeWhitespace();

		List<ConfigClassEntry> content = new ArrayList<>();

		boolean proceed = true;

		while (proceed) {
			switch (reader.peek()) {
			case '{':
				// nested ArrayEntry
				content.add(ArrayEntry.fromText(reader, null, false));
				break;
			case ',':
				// consume comma and all following WS
				reader.read();
				reader.consumeWhitespace();
				break;
			case '}':
				// end of array reached
				reader.read();
				proceed = false;
				break;
			default:
				// has to be a nested ValueEntry
				content.add(ValueEntry.fromText(reader, null));
			}
		}

		return new ArrayStruct(content.toArray(new ConfigClassEntry[content.size()]));
	}

	/**
	 * Gets the length of the represented array-body
	 */
	public int length() {
		return length;
	}

	/**
	 * Gets the actual content of the represented array-body
	 */
	public ConfigClassEntry[] getContent() {
		return Arrays.copyOf(content, content.length);
	}

	@Override
	public String toString() {
		return "ArrayStruct - " + length() + " entries";
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ArrayStruct)) {
			return false;
		}

		ArrayStruct other = (ArrayStruct) o;

		return this.length == other.length && Arrays.deepEquals(this.content, other.content);
	}

	@Override
	public String toText() {
		StringBuilder builder = new StringBuilder();

		builder.append("{");

		for (int i = 0; i < content.length; i++) {
			if (i > 0) {
				builder.append(" ,");
			}
			builder.append(content[i].toText());
		}
		
		builder.append("}");

		return builder.toString();
	}

}
