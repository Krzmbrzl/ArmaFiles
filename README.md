# ArmaFiles
A collection of utlities for dealing with Arma files in Java.

## Usage
### PBO
For accessing a `PBO` you have to create a new PBO-object from a file pointing to the PBO on the hard drive
```Java
PBO pbo = new PBO(new File("<your path here>"));
```
By creating the `PBO` it will also detect all `PBOEntry`s inside it that correspond to the single files inside it. You can get those either via `pbo.getEntries()` or you can search them by name via `pbo.getEntry("<name of the file>");`
A `PBOEntry` can then be used to access the corresponding file's content by providing an `InputStream` to it. In order to get this stream you have to call `entry.toStream()` which will give you a `PBOInputStream` that can be used as any other input stream.

### Config
In order to read in a config file you have to use one of the static methods provided by `ConfigClass`:
```Java
ConfigClass cfgCl = ConfigClass.fromRapifiedFile(new ByteReader(<InputStream to the file>));
ConfigClass cfgCl2 = ConfigClass.fromTextFile(new TextReader(<InputStream to the file>));
```
If you are reading the config out of a PBO the needed `InputStream` can be a `PBOInputStream` as returned by `entry.toStream()`.

So when do you need to use which function? Simple: If the config file that should be read is binarized/rapified (e.g. `config.bin` in addons) you have to use `fromRapifiedFile` whereas `fromTextFile` is the right choice if you're reading the config in from a plain text file. Note though that the input-file for the latter case has to be fully preprocessed!
All entries in a config file are stored as a `ConfigClassEntry` which can either be a `SubclassEntry`, an `ArrayEntry` or a `ValueEntry` (String, Float, Long).
For dealing with a `CfgFunctions` in particular there's a corresponding class for it that can be created. it will offer access to all functions defined in it with their set attributes.
```Java
CfgFunctions cfg = new CfgFunctions(cfgCl);
cfg.init();
HashSet<String, ConfigFunction> definedFunctions = cfg.getDefinedFunctions();
```

### Example
In this example all functions defined inside a mod-folder are being extracted. It is copied from https://github.com/Krzmbrzl/SQDev
``` Java
for (File currentFolder : modFolders) {
	if (modName != null && !currentFolder.getName().toLowerCase().equals(modName)) {
		continue;
	}

	File addonFolder = new File(currentFolder, "addons");

	for (File currentAddon : addonFolder.listFiles()) {
		if (currentAddon.getName().toLowerCase().endsWith(".pbo")) {
			try {
				PBO pbo = new PBO(currentAddon);
				ConfigClass config = null;

				PBOEntry configEntry = pbo.getEntry("config.bin");
				if (configEntry != null) {
					config = ConfigClass.fromRapifiedFile(new ByteReader(configEntry.toStream()));
				} else {
					configEntry = pbo.getEntry("config.cpp");
					if (configEntry == null) {
						configEntry = pbo.getEntry("config.hpp");

						if (configEntry == null) {
							// there seems to be no config-entry for this mod
							continue;
						}

						config = ConfigClass.fromTextFile(new TextReader(configEntry.toStream()));
					}
				}

				ConfigClass functionsConfig = config.getSubclass(CfgFunctions.NAME, false);

				if (functionsConfig != null) {
					CfgFunctions cfg = new CfgFunctions(functionsConfig);
					cfg.init();

					for (ConfigFunction current : cfg.getDefinedFunctions().values()) {
						functions.add(Function.from(current));
					}
				}
			} catch (IOException | RapificationException | ConfigException e) {
				e.printStackTrace();
			}
		}
	}
}
```
