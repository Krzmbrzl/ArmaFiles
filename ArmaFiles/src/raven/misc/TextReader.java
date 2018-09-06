package raven.misc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

public class TextReader implements Closeable {

	/**
	 * The byte source
	 */
	private InputStream source;
	/**
	 * The amount of already read bytes
	 */
	protected int readBytes;

	protected Stack<Integer> unreadStack;

	public TextReader(InputStream in) {
		this.source = in;
		this.readBytes = 0;
		this.unreadStack = new Stack<>();
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
	 * Gets the current position of the reader in the source-stream
	 */
	public int getPosition() {
		return readBytes;
	}

	/**
	 * Reads the next line in the input. The line delimiter is part of the returned
	 * string
	 * 
	 * @return The respective line or an <code>null</code> if the end of the input
	 *         is reached
	 * @throws IOException
	 */
	public String readLine() throws IOException {
		StringBuilder builder = new StringBuilder();

		int c;

		while ((c = read()) != -1) {
			builder.append((char) c);

			if (c == (int) '\n') {
				break;
			}
		}

		return c == -1 && builder.length() == 0 ? null : builder.toString();
	}

	/**
	 * Reads the next word in the input sequence. A word is considered any sequence
	 * of characters that fulfill {@linkplain Character#isJavaIdentifierPart(char)}
	 * 
	 * @return The respective String or <code>null</code> if the end of stream has
	 *         been reached
	 * @throws IOException
	 */
	public String readWord() throws IOException {
		StringBuilder builder = new StringBuilder();

		int c;

		while ((c = read()) != -1 && (Character.isAlphabetic(c) || c == '_' || Character.isDigit(c))) {
			builder.append((char) c);
		}

		unread(c);

		return c == -1 && builder.length() == 0 ? null : builder.toString();
	}

	/**
	 * Reads a String from the input. Note that it has to be properly opened and
	 * closed by either " or ' and double-quotes are being used as quote-escaping.
	 * The wrapping quotation marks are being consumed but not part of the returned
	 * String.
	 * 
	 * @return The respective String (without the wrapping quotation marks) or
	 *         <code>null</code> if the end of stream has been reached
	 * @throws IOException
	 */
	public String readString() throws IOException {
		StringBuilder builder = new StringBuilder();

		char delimiter = (char) read();

		if (delimiter != '"' && delimiter != '\'') {
			throw new IllegalStateException(
					"Tried to read a String but the next character in the sequence is no string-delimiter!");
		}

		char c;
		boolean stringClosed = false;

		while ((c = (char) read()) != (char) -1) {
			if (c == delimiter) {
				char peek = (char) read();

				if (peek == delimiter) {
					// escaped quotation mark -> add single quotation mark
					builder.append(peek);
				} else {
					// the String has been terminated
					unread(peek);
					stringClosed = true;
					break;
				}
			} else {
				builder.append(c);
			}
		}

		if (!stringClosed) {
			throw new IllegalStateException("Encountered unclosed String-definition in input!");
		}

		return c == (char) -1 && builder.length() == 0 ? null : builder.toString();
	}

	/**
	 * Reads a number from the input
	 * 
	 * @return The read number
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public float readNumber() throws IOException {
		StringBuilder builder = new StringBuilder();

		int c;

		while ((c = read()) != -1 && (Character.isDigit(c) || c == '.' || c == '-' || c == '+')) {
			builder.append((char) c);
		}

		unread(c);

		return Float.parseFloat(builder.toString());
	}

	/**
	 * Consumes the next character in the input and checks whether it matches the
	 * given character. The read character is being discarded.
	 * 
	 * @param c
	 *            The character that is expected to be consumed
	 * @throws IOException
	 * @throws IllegalStateException
	 *             If the consumed character does not match the given one
	 */
	public void expect(char c) throws IOException {
		if (c != (char) read()) {
			throw new IllegalStateException("Attempted to consume \"" + c + "\" but failed!");
		}
	}

	/**
	 * Consumes all whitespace characters from the current position on and discards
	 * them
	 * 
	 * @throws IOException
	 */
	public void consumeWhithespace() throws IOException {
		int c;

		while ((c = read()) != -1 && Character.isWhitespace(c)) {
			// simply consume the WS
		}

		// unread last character
		unread(c);
	}

	/**
	 * Peeks at the next character in the stream
	 * 
	 * @return The character that will be returned by the next call to
	 *         {@link #read()}
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

}
