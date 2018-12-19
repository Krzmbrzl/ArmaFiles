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
	 * Creates a new macro
	 * 
	 * @param name
	 *            The name of the macro
	 * @param arguments
	 *            The names of the macro's arguments
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
	 * @return The fully expanded replacement text of this macro
	 */
	public String expand(List<String> arguments, Map<String, Macro> macros) {
		if (arguments.size() != this.argumentNames.size()) {
			// if the argument count doesn't match up -> return an empty String
			// TODO: report error about wrong argument count
			return "";
		}

		if (body.length() == 0) {
			return "";
		}

		String macroContent = body;

		// replace double-hashtags in order for them to not interfere with the
		// stringification
		macroContent = macroContent.replace("##", String.valueOf(Character.MAX_VALUE));

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

		// remove "##", that have been replaced by a single Character.MAX_VALUE, in
		// order to "concatenate"
		macroContent = macroContent.replace(String.valueOf(Character.MAX_VALUE), "");

		return Preprocessor.expandAll(macroContent, macros);
	}

	/**
	 * Gets the number of arguments this macro expects
	 */
	public int getArgumentCount() {
		return argumentNames.size();
	}

}
