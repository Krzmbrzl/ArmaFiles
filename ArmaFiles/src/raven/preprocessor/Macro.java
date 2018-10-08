package raven.preprocessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Macro {
	String name;
	final String body;
	List<String> argumentNames;

	public Macro(String name, List<String> arguments, String body) {
		this.name = name;
		this.argumentNames = arguments;
		this.body = body;
	}

	public String expand(List<String> arguments, Map<String, Macro> macros) {
		if (arguments.size() != this.argumentNames.size()) {
			throw new IllegalArgumentException("The amount of given does not match the macro's definition!");
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

}
