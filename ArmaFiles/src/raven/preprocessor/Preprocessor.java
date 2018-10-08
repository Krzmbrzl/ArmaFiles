package raven.preprocessor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import raven.misc.TextReader;

public class Preprocessor {

	protected TextReader in;
	protected OutputStream out;
	protected Map<String, Macro> macros;
	protected boolean usesWindowsNewline;

	public Preprocessor() {
		macros = new HashMap<>();
	}

	public void preprocess(TextReader in, OutputStream out) throws IOException {
		reset();

		this.in = in;
		this.out = out;

		// prevent thread from terminating because of some error during preprocessing
		try {
			doPreprocess();
		} catch (Exception e) {
			// TODO: create error with the respective exception-message
			e.printStackTrace();
		}
	}

	protected boolean doPreprocess() throws IOException {
		boolean ok = true;

		int c = readNext();
		boolean beginOfInput = true;

		while (c >= 0) {
			if (c == '"') {
				// unread first so the starting quotation mark is the first character the
				// readString-method encounters
				in.unread('"');

				String str = in.readString();

				if (str != null) {
					writeToOut(("\"" + str + "\""));
				}
			} else {
				boolean newLine = c == '\n';

				if (newLine | beginOfInput) {
					while (c == '\n') {
						writeToOut((char) c);

						c = readNext();
					}

					in.unread(c);

					skipWhitespace(false);

					c = readNext();

					if (c == '#') {
						if (Character.isWhitespace(in.peek())) {
							// TODO: error about WS
							skipWhitespace(false);
						}

						String command = in.readWord();

						skipWhitespace(false);

						switch (command) {
						case "include":
							ok = includeBlock();
							break;
						case "define":
							ok = defineBlock();
							break;
						case "ifdef":
							ok = ifDefBlock(true);
							break;
						case "ifndef":
							ok = ifDefBlock(false);
							break;
						case "undef":
							ok = undefBlock();
							break;
						case "endif":
						case "else":
							// TODO: error about orphaned #else or #endif
							ok = false;
							break;
						default:
							// TODO: error about unrecognized preprocessor command
							ok = false;
							break;
						}
						if (!ok)
							return false;
					} else {
						in.unread(c);
					}
				} else if (c == '/') {
					if (in.peek() == '*') {
						// block-comment
						readNext();
						skipBlockComment();
					} else if (in.peek() == '/') {
						// line comment
						readNext();
						skipLineComment();
					} else {
						// it's not a comment
						writeToOut((char) c);
					}
				} else {
					if (Character.isWhitespace(c)) {
						writeToOut((char) c);
					} else {
						// unread first so whole word gets read
						in.unread(c);

						String word = in.readWord();

						if (!word.isEmpty()) {
							potentialMacro(word);
						} else {
							// the current character doesn't appear to be part of a word -> read it in as a
							// single character
							writeToOut((char) readNext());
						}
					}
				}
			}

			c = readNext();
			beginOfInput = false;
		}

		return ok;
	}

	protected void potentialMacro(String word) throws IOException {
		if (macros.containsKey(word)) {
			writeToOut(doExpandMacro(word));
		} else {
			// write to output unchanged
			writeToOut(word);
		}
	}

	protected String doExpandMacro(String macro) throws IOException {
		if (!macros.containsKey(macro)) {
			return macro;
		}

		List<String> arguments = new ArrayList<>();

		if (in.peek() == '(') {
			// there are arguments
			in.expect('(');

			StringBuilder currentArg = new StringBuilder();

			int c = readNext();
			int openParentheses = 0;

			while ((c != ')' || openParentheses > 0) && c >= 0) {
				if (c == '(') {
					openParentheses++;
				}
				if (c == ')') {
					openParentheses--;
				}

				if (c == ',') {
					arguments.add(currentArg.toString());
					currentArg.setLength(0);
				} else {
					currentArg.append((char) c);
				}

				c = readNext();
			}

			arguments.add(currentArg.toString());
			in.unread(c);
			in.expect(')');
		}

		return macros.get(macro).expand(arguments, macros);
	}

	/**
	 * Consumes a line-comment after the starting double-slash has been consumed
	 * (therefore this method will consume everything until a newline character or
	 * the end of input is detected. Note that the newline character itself will not
	 * be consumed)
	 * 
	 * @throws IOException
	 */
	protected void skipLineComment() throws IOException {
		int c = readNext();

		while (c != '\n' && c >= 0) {
			c = readNext();
		}
	}

	protected void skipBlockComment() throws IOException {
		int c = readNext();
		boolean proceed = true;
		boolean foundStar = false;

		while (proceed) {
			proceed = !(foundStar && c == '/') && c >= 0;

			foundStar = c == '*';

			if (c == '\n') {
				writePreservedNewline();
			}

			c = readNext();
		}

		in.unread(c);
	}

	protected boolean undefBlock() throws IOException {
		String macroName = in.readWord();

		macros.remove(macroName);

		return false;
	}

	protected boolean ifDefBlock(boolean hasToBeDefined) throws IOException {
		String macroName = in.readWord();

		boolean firstIfBlock = hasToBeDefined == macros.containsKey(macroName);

		StringBuilder relevantBlock = new StringBuilder();

		boolean inRelevantBlock = firstIfBlock;
		int c = readNext();
		boolean newLine = c == '\n';

		while (c >= 0) {
			if (newLine) {
				if (Character.isWhitespace(c)) {
					if (inRelevantBlock && c == '\n') {
						relevantBlock.append((char) c);
					}

					// "skip" WS at line-start
					c = readNext();

					if (Character.isWhitespace(c)) {
						continue;
					}
				}
				if (c == '#') {
					String command = in.readWord();

					if (command.equals("else")) {
						inRelevantBlock = !inRelevantBlock;
					}
					if (command.equals("endif")) {
						break;
					}
				} else {
					if (inRelevantBlock) {
						relevantBlock.append((char) c);
					}
				}
			} else {
				if (inRelevantBlock) {
					relevantBlock.append((char) c);
				}
			}

			newLine = c == '\n';
			c = readNext();
		}

		relevantBlock = relevantBlock.reverse();
		for (int i = 0; i < relevantBlock.length(); i++) {
			in.unread((int) relevantBlock.charAt(i));
		}

		return true;
	}

	protected boolean defineBlock() throws IOException {
		String macroName = in.readWord();
		List<String> arguments = new ArrayList<>();

		if (in.peek() == '(') {
			// parse arguments
			in.expect('(');

			String argumentName = in.readWord();

			while (argumentName != null && !argumentName.isEmpty()) {
				arguments.add(argumentName);

				if (in.peek() == ',') {
					// there's another macro
					in.expect(',');
					argumentName = in.readWord();
				} else {
					// no further arguments
					argumentName = null;
				}
			}

			in.expect(')');
		}

		if (in.peek() == ' ') {
			// remove leading space
			readNext();
		}

		StringBuilder macroBody = new StringBuilder();

		int c;
		boolean foundBackslash = false;

		while (((c = readNext()) != '\n' || foundBackslash) && c >= 0) {
			if (foundBackslash && c == '\n') {
				// remove the backslash as it was only to escape the NL
				macroBody.setLength(macroBody.length() - 1);

				writePreservedNewline();
			} else {
				// escaped NLs aren't part of the macro body
				macroBody.append((char) c);
			}

			foundBackslash = c == '\\';
		}

		in.unread(c);

		if (macros.containsKey(macroName)) {
			// TODO: error that the macros has already been defined with the given amount of
			// arguments

			return false;
		}

		Macro macro = new Macro(macroName, arguments, macroBody.toString());

		macros.put(macroName, macro);

		return true;
	}

	protected boolean includeBlock() throws IOException {
		String path = in.readString();

		System.err.println("Including \"" + path + "\"");
		// TODO

		return true;
	}

	/**
	 * Reads the next character in the input. This method does skip
	 * carriage-return-characters completely
	 * 
	 * @return The read character
	 * @throws IOException
	 */
	protected int readNext() throws IOException {
		int c = in.read();

		if (c == '\r') {
			c = in.read();
			usesWindowsNewline = true;
		}

		return c;
	}

	/**
	 * Skips all whitespace
	 * 
	 * @param writeAll
	 *            Indicates whether all WS should get written to {@link #out}
	 *            instead of NL only
	 * 
	 * @throws IOException
	 */
	protected void skipWhitespace(boolean writeAll) throws IOException {
		int c = readNext();

		while (Character.isWhitespace(c)) {
			if (writeAll || c == '\n') {
				writeToOut((char) c);
			}

			c = readNext();
		}

		in.unread(c);
	}

	public Map<String, Macro> getDefinedMacros() {
		return new HashMap<>(macros);
	}

	public void reset() {
		in = null;
		out = null;
		macros.clear();
	}

	protected void writePreservedNewline() throws IOException {
		if (usesWindowsNewline) {
			out.write('\r');
		}
		out.write('\n');
	}

	protected void writeToOut(String content) throws IOException {
		if (usesWindowsNewline) {
			content = content.replace("\n", "\r\n");
		}
		out.write(content.getBytes());
	}

	protected void writeToOut(char c) throws IOException {
		if (c == '\n' && usesWindowsNewline) {
			out.write('\r');
		}
		out.write(c);
	}

	/**
	 * Expands the given content completely based on the given list of macros
	 * 
	 * @param content
	 *            The content to expand
	 * @param macros
	 *            The {@linkplain List} of macros to be used for the expansion
	 * @return The fully expanded content
	 */
	protected static String expandAll(String content, Map<String, Macro> macros) {
		StringBuilder expandedContent = new StringBuilder();
		StringBuilder currentWord = new StringBuilder();

		boolean foundHashtag = false;
		boolean foundQuotationMark = false;

		for (int i = 0; i < content.length(); i++) {
			char c = content.charAt(i);

			if (foundQuotationMark) {
				expandedContent.append(c);

				if (c == '"') {
					foundQuotationMark = false;
				}

				// don't process content in Strings with double quotes
				continue;
			}

			if (Character.isJavaIdentifierPart(c)) {
				currentWord.append(c);
			} else {
				int before = i;
				i = tryExpand(content, foundHashtag, currentWord, expandedContent, c, i, macros);

				if (i != before) {
					// continue with the new current character as tryExpand performed some magic
					continue;
				}

				foundHashtag = false;

				if (c != '#') {
					// add the current character to the expanded macro
					if (c != Character.MIN_VALUE) {
						expandedContent.append(c);
					}
					foundHashtag = false;
				} else {
					foundHashtag = true;
				}

				foundQuotationMark = c == '"';
			}
		}

		tryExpand(content, foundHashtag, currentWord, expandedContent, Character.MIN_VALUE, content.length(), macros);

		return expandedContent.toString();
	}

	static protected int tryExpand(String content, boolean foundHashtag, StringBuilder currentWord,
			StringBuilder expandedContent, char c, int i, Map<String, Macro> macros) {
		if (foundHashtag) {
			expandedContent.append("\"");
		}

		if (currentWord.length() > 0) {
			if (macros.containsKey(currentWord.toString())) {
				// this is a macro that has to be expanded
				List<String> argList = new ArrayList<>();

				if (c == '(') {
					// the macro has arguments
					StringBuilder currentArg = new StringBuilder();

					// skip the already encountered opening brace
					i++;
					for (; i < content.length(); i++) {
						// use same counter for iterating through the String
						c = content.charAt(i);

						if (c == ')') {
							break;
						}
						if (c == ',') {
							argList.add(currentArg.toString());
							currentArg.setLength(0);
						} else {
							currentArg.append(c);
						}
					}

					argList.add(currentArg.toString());
				}

				// expand macro and append the expanded text
				expandedContent.append(macros.get(currentWord.toString()).expand(argList, macros));
				// The macro has been expanded so there is no need to add currentWord
				currentWord.setLength(0);
			}
		}

		expandedContent.append(currentWord);
		currentWord.setLength(0);

		if (foundHashtag) {
			expandedContent.append("\"");
		}

		return i;
	}
}
