package raven.config;

import java.util.HashMap;
import java.util.Map;

public class CfgFunctions extends ConfigClass {

	/**
	 * The name of this class
	 */
	public static final String NAME = "CfgFunctions";


	/**
	 * A map to keep track of all functions defined in this CfgFunctions
	 */
	protected Map<String, ConfigFunction> functionMap;

	public CfgFunctions(ConfigClass input) throws ConfigException {
		super(NAME, input.getParentClass(), input.getEntries());

		if (!input.getName().equals(NAME)) {
			throw new ConfigException("Can't create a CfgFunctions of a class whose name isn't CfgFunctions!");
		}

		functionMap = new HashMap<>();
	}

	/**
	 * Tries to locate a CfgFunctions-class in the given Config class. This is done
	 * by first checking the class itself and after that consecutively checking all
	 * entries in this class (and subclasses)
	 * 
	 * @param cl
	 *            The {@linkplain ConfigClass} to search
	 * @return The found {@linkplain CfgFunctions} or <code>null</code> if none
	 *         could be found
	 * @throws ConfigException
	 */
	public static CfgFunctions locate(ConfigClass cl) throws ConfigException {
		if (cl.hasName() && cl.getName().equals(NAME)) {
			return new CfgFunctions(cl);
		}

		for (ConfigClassEntry currentEntry : cl.getEntries()) {
			if (currentEntry instanceof SubclassEntry && !((SubclassEntry) currentEntry).isExtern()) {
				ConfigClass refClass = ((SubclassEntry) currentEntry).getReferencedClass();

				CfgFunctions cfg = locate(refClass);

				if (cfg != null) {
					return cfg;
				}
			}
		}

		return null;
	}

	public void init() throws CfgFunctionsException {
		for (ConfigClassEntry currentTagEntry : getEntries()) {
			if (!(currentTagEntry instanceof SubclassEntry) || ((SubclassEntry) currentTagEntry).isExtern()) {
				throw new CfgFunctionsException("CfgFunctions may only contain explicit (non-extern) subclasses!");
			}

			ConfigClass tagClass = ((SubclassEntry) currentTagEntry).getReferencedClass();
			String tag = tagClass.getName();

			for (ConfigClassEntry currentCategoryEntry : tagClass.getEntries()) {
				if (!(currentCategoryEntry instanceof SubclassEntry
						|| !((SubclassEntry) currentCategoryEntry).isExtern())) {
					throw new CfgFunctionsException(
							"Tag-class in CfgFunctions may only contain explicit (non-extern) subclasses!");
				}

				ConfigClass categoryClass = ((SubclassEntry) currentCategoryEntry).getReferencedClass();
				String pathPrefix = "functions/" + categoryClass.getName();

				// check for tag-overwrite
				ConfigClassEntry tagEntry = categoryClass.getField("tag", false);
				if (tagEntry != null && tagEntry instanceof ValueEntry
						&& ((ValueEntry) tagEntry).getDataType() == ValueEntry.STRING) {
					tag = ((ValueEntry) tagEntry).getString();
				}

				// check for file-specification
				ConfigClassEntry fileEntry = categoryClass.getField("file", false);
				if (fileEntry != null && fileEntry instanceof ValueEntry
						&& ((ValueEntry) fileEntry).getDataType() == ValueEntry.STRING) {
					pathPrefix = ((ValueEntry) fileEntry).getString();
				}

				for (ConfigClassEntry currentFunctionEntry : categoryClass.getEntries()) {
					if (!(currentFunctionEntry instanceof SubclassEntry)) {
						// ignore
						continue;
					} else {
						ConfigClass functionClass = ((SubclassEntry) currentFunctionEntry).getReferencedClass();

						String functionName = tag + "_fnc_" + functionClass.getName();

						// search for extension definition
						String extension = ".sqf";
						ConfigClassEntry extensionEntry = functionClass.getField("ext", false);
						if (extension != null && extensionEntry instanceof ValueEntry
								&& ((ValueEntry) extensionEntry).getDataType() == ValueEntry.STRING) {
							extension = ((ValueEntry) extensionEntry).getString();
						}

						String path;
						ConfigClassEntry functionFileEntry = functionClass.getField("file", false);
						if (functionFileEntry != null && functionFileEntry instanceof ValueEntry
								&& ((ValueEntry) functionFileEntry).getDataType() == ValueEntry.STRING) {
							// use file specified for function
							path = ((ValueEntry) functionFileEntry).getString();
						} else {
							// use pathPrefix and className
							path = pathPrefix + "fn_" + functionClass.getName() + extension;
						}

						// assemble all attributes
						FieldEntry[] fields = functionClass.getFields();
						String[] attributes = new String[fields.length];

						for (int i = 0; i < fields.length; i++) {
							attributes[i] = fields[i].toText();
						}

						// store function
						functionMap.put(functionName.toLowerCase(), new ConfigFunction(functionName, path, attributes));
					}


				}

				// reset tag and pathPrefix in case it got overwritten
				tag = tagClass.getName();
				pathPrefix = "functions/" + categoryClass.getName();
			}
		}
	}

	/**
	 * Gets a map of all functions that are defined in this CfgFunctions
	 */
	public Map<String, ConfigFunction> getDefinedFunctions() {
		return new HashMap<>(functionMap);
	}
}
