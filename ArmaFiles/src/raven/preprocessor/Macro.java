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

		// replace arguments in body
		for (int i = 0; i < argumentNames.size(); i++) {
			String currentPlaceholder = argumentNames.get(i);

			Pattern p = Pattern.compile("(((?:^| |;)|##)(" + Pattern.quote(currentPlaceholder)
					+ ")((?=;| |$)|##))|(?:#)(" + Pattern.quote(currentPlaceholder) + ")((?:;| |$)|##)");
			Matcher matcher = p.matcher(macroContent);

			int startindex = 0;

			while (matcher.find(startindex)) {
				int start = matcher.start(1);
				int end = matcher.end(1);

				if (start < 0) {
					// the second alternative matched -> use group 5
					start = matcher.start(5);
					end = matcher.end(5) ;
				}

				macroContent = macroContent.substring(0, start) + arguments.get(i) + macroContent.substring(end);

				startindex = start + arguments.get(i).length();

				matcher = p.matcher(macroContent);
			}
		}

		// replace any redundant ##
		macroContent = macroContent.replace("##", "");

		return Preprocessor.expandAll(macroContent, macros);
	}

}
