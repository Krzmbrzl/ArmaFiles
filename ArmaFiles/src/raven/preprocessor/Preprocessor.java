package raven.preprocessor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import raven.misc.TextReader;


/**
 * A custom implementation of the Arma-preprocessor
 * 
 * @author Raven
 *
 */
public class Preprocessor {
	/**
	 * The {@linkplain TextReader} used as an input source
	 */
	protected TextReader in;
	/**
	 * The {@linkplain OutputStream} the preprocessed content should be written to
	 */
	protected OutputStream out;
	/**
	 * A map of defined macros
	 */
	protected Map<String, Macro> macros;
	/**
	 * A flag indicating whether the input format uses windows-newlines (that is
	 * \r\n instead if simply \n)
	 */
	protected boolean usesWindowsNewline;
	/**
	 * An option indicating how to deal with errors caused by misplaced whitespace
	 */
	protected PreprocessorWhitespaceHandling wsHandling;
	/**
	 * An option indicating whether the bugs of the Arma-preprocessor should be
	 * reproduced
	 */
	protected PreprocessorBugReproduction bugReproduction;


	/**
	 * Creates a new instance of this preprocessor
	 * 
	 * @param wsHandling
	 *            How to deal with errors concerning misplaced whitespace
	 * @param bugReproduction
	 *            Whether or not to reproduce the bugs of the Arma-preprocessor
	 * @see PreprocessorWhitespaceHandling
	 * @see PreprocessorBugReproduction
	 */
	public Preprocessor(PreprocessorWhitespaceHandling wsHandling, PreprocessorBugReproduction bugReproduction) {
		macros = new HashMap<>();
		this.wsHandling = wsHandling;
		this.bugReproduction = bugReproduction;
	}

	/**
	 * Creates a new instance of this preprocessor that doesn't bail out on
	 * misplaced whitespace and won't reproduce any Arma-bugs
	 */
	public Preprocessor() {
		this(PreprocessorWhitespaceHandling.TOLERANT, PreprocessorBugReproduction.NONE);
	}

	/**
	 * Preprocesses the given content
	 * 
	 * @param in
	 *            The {@linkplain TextReader} that will be used as the content-input
	 * @param out
	 *            The {@linkplain OutputStream} the preprocessed content should be
	 *            written to
	 * @throws IOException
	 */
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

	/**
	 * Internal method for actually doing the whole preprocessing steps
	 * 
	 * @return Whether or not the preprocessing has been successful
	 * @throws IOException
	 */
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
							if (wsHandling == PreprocessorWhitespaceHandling.STRICT) {
								throw new IllegalArgumentException("Whitespace in invalid context!");
							} else {
								// TODO: error about WS
								skipWhitespace(false);
							}
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

	/**
	 * A function that is being called whenever the given word might be a potential
	 * macro. If it is, it will be expanded and its expanded replacement text
	 * written to {@link #out}. If not, the word is written to {@link #out}
	 * unchanged
	 * 
	 * @param word
	 *            The word suspected of being a macro
	 * @throws IOException
	 */
	protected void potentialMacro(String word) throws IOException {
		if (macros.containsKey(word)) {
			writeToOut(doExpandMacro(word));
		} else {
			// write to output unchanged
			writeToOut(word);
		}
	}

	/**
	 * Tries to expand the the macro with the given name. If a macro with the given
	 * name is found, potential arguments will be read from {@link #in}
	 * 
	 * @param macroName
	 *            The name of the macro to expand
	 * @return The expanded macro replacement text or the given macroName if it was
	 *         found to not be a macro
	 * @throws IOException
	 */
	protected String doExpandMacro(String macroName) throws IOException {
		if (!macros.containsKey(macroName)) {
			return macroName;
		}

		Macro macro = macros.get(macroName);

		List<String> arguments = null;

		// only search for arguments if the macro actually expects them
		if (macro.expectsArguments() && in.peek() == '(') {
			arguments = new ArrayList<>();
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

			if (c != ')') {
				// there's something wrong
				if (c == -1) {
					// If the macro isn't closed before the EOF Arma pretends as if the macro was
					// used with an empty argument
					arguments.add("");
				} else {
					throw new IllegalStateException("Unhandled unclosed macro!");
				}
			} else {
				// everything is okay -> add argument
				arguments.add(currentArg.toString());
			}
		}

		if (!macro.isValid() || (macro.expectsArguments() && arguments != null)
				|| (!macro.expectsArguments() && arguments == null)) {
			return macro.expand(arguments, macros, in.peek() == -1,
					bugReproduction == PreprocessorBugReproduction.ARMA);
		} else {
			return macroName;
		}
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
		// TODO: ad option to keep comments
		int c = readNext();

		while (c != '\n' && c >= 0) {
			c = readNext();
		}
	}

	/**
	 * Consumes a block comment. This method assumes that the block comment has been
	 * started already and thus is only searching for the end of it
	 * 
	 * @throws IOException
	 */
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

	/**
	 * Handles an undefine-block after the #undef has already been consumed
	 * 
	 * @return Whether the undefine has been performed successfully
	 * @throws IOException
	 */
	protected boolean undefBlock() throws IOException {
		String macroName = in.readWord();

		// TODO: warning about undefining a macro that doesn't even exist

		macros.remove(macroName);

		return true;
	}

	/**
	 * Handles an ifdef or ifbdef block after the {@link #ifDefBlock(boolean)}or
	 * #ifndef has already been consumed
	 * 
	 * @param hasToBeDefined
	 *            Whether #ifdef has been used
	 * @return Whether the block was processed successfully
	 * @throws IOException
	 */
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

	/**
	 * Processes a define-block after the #define has already been consujmed
	 * 
	 * @return Whether the define-block has been processed successfully
	 * @throws IOException
	 */
	protected boolean defineBlock() throws IOException {
		String macroName = in.readWord();
		List<String> arguments = null;
		boolean valid = true;

		if (in.peek() == '(') {
			arguments = new ArrayList<>();
			// parse arguments
			in.expect('(');

			// skip WS as leading WS gets removed by Arma
			skipWhitespace(false);

			String argumentName = in.readWord();

			while (argumentName != null && !argumentName.isEmpty()) {
				if (in.peek() == ',' || in.peek() == ')') {
					// add valid argument
					arguments.add(argumentName);
				} else {
					skipWhitespace(false);
					if (in.peek() != ')') {
						// TODO: Error about invalid macro definition
						// this macro definition is invalid
						valid = false;

						// read all characters until ")" is encountered
						int c = in.read();
						while (c != ')' && c != -1) {
							c = in.read();
						}

						// unread last character
						in.unread(c);
					} else {
						// The problem was just some trailing space which gets ignored anyways -> add
						// the argument anyways
						arguments.add(argumentName);
					}
				}

				if (in.peek() == ',') {
					// there's another macro
					in.expect(',');
					argumentName = in.readWord();

					if (argumentName != null && argumentName.isEmpty()) {
						if (Character.isWhitespace(in.peek())) {
							skipWhitespace(false);
							valid = false;
						}
					}
				} else {
					// no further arguments
					argumentName = null;
				}
			}

			// skip WS as this gets trimmed away from the macro argument by Arma
			skipWhitespace(false);

			if (in.peek() == ')') {
				in.expect(')');
			} else {
				// TODO: error about unclosed macro definition
				System.err.println("Unlosed Macro definition!");
			}
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

		Macro macro = new Macro(macroName, arguments, macroBody.toString(), valid);

		macros.put(macroName, macro);

		return true;
	}

	/**
	 * Handles an include-statement after the #inlude has already been consumed
	 * 
	 * @return Whether the inclusion has been performed successfully
	 * @throws IOException
	 */
	protected boolean includeBlock() throws IOException {
		String path = in.readString();

		System.err.println("Including \"" + path + "\"");
		// TODO

		return false;
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

	/**
	 * Gets a {@linkplain Map} of the macros that have been defined during the last
	 * run of {@link #preprocess(TextReader, OutputStream)}
	 */
	public Map<String, Macro> getDefinedMacros() {
		return new HashMap<>(macros);
	}

	/**
	 * Resets this preprocessor to its initial state. This method gets automatically
	 * called inside {@link #preprocess(TextReader, OutputStream)}
	 */
	public void reset() {
		in = null;
		out = null;
		macros.clear();
		usesWindowsNewline = false;
	}

	/**
	 * Internal method for writing a newline to the output stream. This method
	 * automatically handles whether a \n or a \r\n has to be inserted to the output
	 * stream
	 * 
	 * @throws IOException
	 */
	protected void writePreservedNewline() throws IOException {
		if (usesWindowsNewline) {
			out.write('\r');
		}
		out.write('\n');
	}

	/**
	 * Writes the given content to {@link #out}. This method automatically checks
	 * whether it has to insert \n or \r\n as newlines
	 * 
	 * @param content
	 *            The content to be written to {@link #out}. It must not contain
	 *            \r\n newlines
	 * @throws IOException
	 */
	protected void writeToOut(String content) throws IOException {
		if (usesWindowsNewline) {
			content = content.replace("\n", "\r\n");
		}
		out.write(content.getBytes());
	}

	/**
	 * Writes a single character to {@link #out}. If the character is \n it
	 * automatically checks whether it has to write an \r beforehand.
	 * 
	 * @param c
	 *            The character to write to {@link #out}
	 * @throws IOException
	 */
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

	/**
	 * Tries to expand the given content
	 * 
	 * @param content
	 *            The content to be expanded
	 * @param foundHashtag
	 *            Whether or not a hashtag has been found before
	 * @param currentWord
	 *            The currently processed word
	 * @param expandedContent
	 *            The so-far expanded content
	 * @param c
	 *            The current character
	 * @param i
	 *            The current index of the "reader" in content
	 * @param macros
	 *            The {@linkplain Map} with all the macros that have been defined
	 * @return The new index i of the "reader" inside the given content
	 */
	protected static int tryExpand(String content, boolean foundHashtag, StringBuilder currentWord,
			StringBuilder expandedContent, char c, int i, Map<String, Macro> macros) {
		if (foundHashtag) {
			expandedContent.append("\"");
		}

		if (currentWord.length() > 0) {
			if (macros.containsKey(currentWord.toString())) {
				Macro currentMacro = macros.get(currentWord.toString());
				// this is a macro that has to be expanded
				List<String> argList = null;

				if (c == '(') {
					if (currentMacro.expectsArguments()) {
						argList = new ArrayList<>();
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
					} else {
						// The macro doesn't expect arguments so the brace doesn't belong to the macro
						// call
						// "unread" the opening brace
						i--;
					}
				}

				// expand macro and append the expanded text
				// As there can't be EOF after this macro the Arma-EOF-bug does not need to be
				// reproduced
				expandedContent.append(macros.get(currentWord.toString()).expand(argList, macros, false, false));
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

	/**
	 * Determine how errors about misplaced WS should be dealt with.
	 * 
	 * @param wsHandling
	 *            The error handling strategy to be used
	 * @see PreprocessorWhitespaceHandling
	 */
	public void setWhitespaceHandling(PreprocessorWhitespaceHandling wsHandling) {
		this.wsHandling = wsHandling;
	}

	/**
	 * Determines whether bugs of the Arma-preprocessor should be reproduced
	 * 
	 * @param bugReproduction
	 *            The respective option
	 * @see PreprocessorBugReproduction
	 */
	public void setBugReprodiction(PreprocessorBugReproduction bugReproduction) {
		this.bugReproduction = bugReproduction;
	}
}
