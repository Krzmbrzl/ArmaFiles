package raven.pbo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An {@linkplain InputStream} reading the content of a file inside a PBO-file
 * 
 * @author Raven
 *
 */
public class PBOInputStream extends InputStream {

	/**
	 * The {@linkplain PBOEntry} this stream corresponds to
	 */
	protected PBOEntry entry;
	/**
	 * The internal stream used to access the data
	 */
	protected FileInputStream internalStream;
	/**
	 * The amount of already read bytes
	 */
	protected int readBytes;


	/**
	 * Creates a new instance of this stream working on the given
	 * {@linkplain PBOEntry}
	 * 
	 * @param entry
	 *            The entry to work on
	 * @throws IOException
	 *             If there are any errors during reading
	 * @throws IllegalStateException
	 *             If the provided entry is a boundary entry
	 */
	public PBOInputStream(PBOEntry entry) throws IOException {
		this.entry = entry;

		if (entry.isBoundary()) {
			throw new IllegalStateException("Can't create a stream of a boundary entry!");
		}

		internalStream = new FileInputStream(entry.getPBO().toFile());
		readBytes = 0;

		pointStream();
	}

	/**
	 * Points the internal stream to the beginning of the file represented by the
	 * {@link #entry} inside the PBO
	 * 
	 * @throws IOException
	 */
	protected void pointStream() throws IOException {
		internalStream.getChannel().position(entry.getStartOffset());
	}

	@Override
	public int read() throws IOException {
		if (readBytes >= entry.getDataSize()) {
			return -1;
		}

		readBytes++;

		return internalStream.read();
	}

	/**
	 * Reads the content of the file represented by the set {@linkplain PBOEntry} as
	 * a String by simply casting all read bytes as a char and appending them to a
	 * {@linkplain CharSequence}
	 * 
	 * @return The read String
	 * @throws IOException
	 */
	public String readAll() throws IOException {
		StringBuilder builder = new StringBuilder();

		int c;
		while ((c = read()) >= 0) {
			builder.append((char) c);
		}

		return builder.toString();
	}

	/**
	 * This method closes the internal {@linkplain InputStream}
	 */
	@Override
	public void close() throws IOException {
		internalStream.close();
	}

}
