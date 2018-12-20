package raven.preprocessor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class representing a macro as used by {@linkplain Preprocessor}. It can be
 * expanded in order to get its replacement text
 * 
 * @author Raven
 *
 */
public class Macro {
	/**
	 * The name of this macro
	 */
	protected String name;
	/**
	 * The body of this macro that will represent its replacement text once all
	 * arguments have been applied
	 */
	protected final String body;
	/**
	 * The names of the arguments for this macros. If those names are present inside
	 * {@link #body}, they will be replaced by the submitted arguments on expansion
	 */
	protected List<String> argumentNames;
	/**
	 * A flag indicating whether this macro is valid. Invalid macros are ones that
	 * have been defined wrongly e.g. #define MACRO()
	 */
	protected boolean isValid;
	/**
	 * A flag indicating whether this macro has been used properly the last time
	 */
	protected boolean validUse;

	/**
	 * Creates a new macro
	 * 
	 * @param name
	 *            The name of the macro
	 * @param arguments
	 *            The names of the macro's arguments. If this macro was defined
	 *            without arguments this has to be <code>null</code>. If the macro
	 *            was defined with an empty set of arguments this has to be an empty
	 *            list.
	 * @param body
	 *            The macro body
	 */
	public Macro(String name, List<String> arguments, String body) {
		this(name, arguments, body, true);
	}

	/**
	 * Creates a new macro
	 * 
	 * @param name
	 *            The name of the macro
	 * @param arguments
	 *            The names of the macro's arguments
	 * @param body
	 *            The macro body
	 * @param isValid
	 *            Whether this macro has been defined properly
	 */
	public Macro(String name, List<String> arguments, String body, boolean isValid) {
		this.name = name;
		this.argumentNames = arguments;
		this.body = body;
		this.isValid = isValid;
	}

	/**
	 * Expands this macro
	 * 
	 * @param arguments
	 *            The arguments supplied for the replacement inside the macro body
	 * @param macros
	 *            The {@linkplain Map} containing all currently defined macros
	 * @param followedByEOF
	 *            A flag indicating whether this macro call is being followed by the
	 *            EOF
	 * @return The fully expanded replacement text of this macro
	 */
	public String expand(List<String> arguments, Map<String, Macro> macros, boolean followedByEOF) {
		String replacementText = doExpand(arguments, macros);

		if (!wasValidUsage() && followedByEOF) {
			// recreate buggy behavior of Arma when using invalid macros right before EOF
			return replacementText + ")";
		} else {
			return replacementText;
		}
	}

	/**
	 * Expands this macro
	 * 
	 * @param arguments
	 *            The arguments supplied for the replacement inside the macro body
	 * @param macros
	 *            The {@linkplain Map} containing all currently defined macros
	 * @return The fully expanded replacement text of this macro
	 */
	protected String doExpand(List<String> arguments, Map<String, Macro> macros) {
		validUse = isValid;

		if (!validUse) {
			// an invalidly defined macro will always return an empty String
			return "";
		}

		if (expectsArguments()) {
			if (arguments.size() != this.argumentNames.size()) {
				if (this.argumentNames.size() == 0 && arguments.size() == 1 && arguments.get(0).isEmpty()) {
					// this is the same as supplying no argument at all -> clear list and continue
					arguments.clear();
				} else {
					// if the argument count doesn't match up -> return an empty String
					validUse = false;
					// TODO: report error about wrong argument count
					return "";
				}
			}
		} else {
			if (arguments != null) {
				throw new IllegalArgumentException("The macro \"" + name + "\" must not be called with arguments!");
			}
		}

		if (body.length() == 0) {
			return "";
		}

		String macroContent = body;

		// replace double-hashtags in order for them to not interfere with the
		// stringification
		macroContent = macroContent.replace("##", String.valueOf(Character.MAX_VALUE));

		if (expectsArguments()) {
			// replace arguments in body
			for (int i = 0; i < argumentNames.size(); i++) {
				String currentPlaceholder = argumentNames.get(i);

				Pattern stringifyPattern = Pattern.compile("(?:[^#]|^)(#\\w*)");
				Matcher stringifyMatcher = stringifyPattern.matcher(macroContent);

				int startIndex = 0;

				while (stringifyMatcher.find(startIndex)) {
					int start = stringifyMatcher.start(1);
					int end = stringifyMatcher.end(1);
					String stringifyArgument = stringifyMatcher.group(1);

					// remove leading #
					stringifyArgument = stringifyArgument.substring(1);

					// insert into content but wrap in quotes
					macroContent = macroContent.substring(0, start) + "\"" + stringifyArgument + "\""
							+ macroContent.substring(end);

					stringifyMatcher = stringifyPattern.matcher(macroContent);
				}

				// replace arguments
				Pattern argumentPattern = Pattern.compile("\\b" + Pattern.quote(currentPlaceholder) + "\\b");
				Matcher matcher = argumentPattern.matcher(macroContent);
				macroContent = matcher.replaceAll(Preprocessor.expandAll(arguments.get(i), macros));
			}
		}

		// remove "##", that have been replaced by a single Character.MAX_VALUE, in
		// order to "concatenate"
		macroContent = macroContent.replace(String.valueOf(Character.MAX_VALUE), "");

		return Preprocessor.expandAll(macroContent, macros);
	}

	/**
	 * Gets the number of arguments this macro expects. Note that there is a
	 * difference between a macro expecting no arguments and a macro expecting
	 * exactly zero arguments. This determines whether MACRO() is actually a macro
	 * call or not. <br>
	 * This function returns 0 in either case. Use {@link #expectsArguments()} to
	 * disambiguate those cases.
	 */
	public int getArgumentCount() {
		return argumentNames == null ? 0 : argumentNames.size();
	}

	/**
	 * Checks whether this macro expects arguments at all. If this is the case a
	 * call like MACRO(content) is considered a macro call even if content is empty.
	 * If this function returns false the part in parenthesis is not part of the
	 * macro call.
	 */
	public boolean expectsArguments() {
		return argumentNames != null;
	}

	/**
	 * Checks whether the last call to {@link #expand(List, Map)} resulted in a
	 * valid expansion
	 */
	public boolean wasValidUsage() {
		return validUse;
	}

	/**
	 * Checks whether this macro is valid in general
	 */
	public boolean isValid() {
		return isValid;
	}

}
