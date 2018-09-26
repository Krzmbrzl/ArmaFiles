package raven.misc;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Stack;

/**
 * A reader that can read certain data types out of an InputStream
 * 
 * @author Raven
 *
 */
public class ByteReader implements Closeable {

	/**
	 * The byte source
	 */
	private InputStream source;
	/**
	 * The amount of already read bytes
	 */
	protected int readBytes;

	protected Stack<Integer> unreadStack;


	/**
	 * Constructs a new instance of this reader based on the given
	 * {@linkplain InputStream}
	 * 
	 * @param in
	 *            The {@linkplain InputStream} to use as a data source
	 */
	public ByteReader(InputStream in) {
		this.source = new BufferedInputStream(in);
		readBytes = 0;
		unreadStack = new Stack<>();
	}

	/**
	 * Redirects to {@linkplain InputStream#read()}
	 * 
	 * @return The next byte of data
	 * @throws IOException
	 */
	public int read() throws IOException {
		readBytes++;

		if (unreadStack.size() == 0) {
			return source.read();
		} else {
			return unreadStack.pop();
		}
	}

	/**
	 * Unreads the given character making it the next one returned by a call to
	 * {@link #read()}. As this method assumes that the given character has
	 * previously been read from this reader, it will also decrease the position by
	 * one
	 * 
	 * @param c
	 *            The character to unread
	 */
	public void unread(int c) {
		readBytes--;

		unreadStack.push(c);
	}

	/**
	 * Redirects to {@linkplain InputStream#read()} but converts its outcome to a
	 * byte
	 * 
	 * @return The next byte of data
	 * @throws IOException
	 */
	public byte readByte() throws IOException {
		return (byte) read();
	}

	/**
	 * Reads a String from the source-stream. That is a sequence of characters until
	 * a zero-byte is being encountered
	 * 
	 * @return The read String (empty if the next read character directly was a
	 *         zero-byte)
	 * @throws IOException
	 */
	public String readString() throws IOException {
		StringBuilder builder = new StringBuilder();

		byte b;
		while ((b = readByte()) != 0) {
			builder.append((char) b);
		}

		return builder.toString();
	}

	/**
	 * Reads 4 bytes as an integer. The last of those four bytes will be used as the
	 * highest digits and the first as the lowest (little endian encoding).
	 * 
	 * @return The converted integer
	 * @throws IOException
	 */
	public int readInt32() throws IOException {
		byte[] bytes = new byte[4];

		for (int i = 0; i < 4; i++) {
			bytes[i] = (byte) read();
		}

		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
	}

	public float readFloat() throws IOException {
		byte[] bytes = new byte[4];

		for (int i = 0; i < 4; i++) {
			bytes[i] = readByte();
		}

		return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	/**
	 * Reads an integer value from the source-stream that has been compressed as a
	 * Variable-length quantity (little endian encoding)
	 * 
	 * @return The read integer
	 * @throws IOException
	 */
	public int readCompressedInt() throws IOException {
		int result;
		int temp;
		int shift = 0;

		result = 0;

		for (int i = 0; i <= 4; i++) {
			temp = read();
			result = result | ((temp & 0x7f) << shift);

			if (temp < 0x80)
				break;

			shift += 7;
		}

		return result;
	}

	/**
	 * Gets the current position of the reader in the source-stream
	 */
	public int getPosition() {
		return readBytes;
	}

	/**
	 * Peeks at the next byte in the stream
	 * 
	 * @return The byte that will be returned by the next call to {@link #read()}
	 * @throws IOException
	 */
	public int peek() throws IOException {
		int c = read();
		unread(c);

		return c;
	}

	@Override
	public void close() throws IOException {
		this.source.close();
	}

	/**
	 * Skips the given amount of bytes
	 * 
	 * @param amount
	 *            The amount of bytes to skip
	 * @throws IOException
	 */
	public void skip(int amount) throws IOException {
		for (int i = 0; i < amount; i++) {
			read();
		}
	}

}
