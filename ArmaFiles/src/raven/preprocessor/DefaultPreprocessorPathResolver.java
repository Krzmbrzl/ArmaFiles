package raven.preprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

public class DefaultPreprocessorPathResolver implements IPreprocessorPathResolver {

	/**
	 * The current root path
	 */
	protected Path currentRoot;

	public DefaultPreprocessorPathResolver(Path root) {
		setCurrentRoot(root);
	}

	@Override
	public InputStream getStream(String path) throws FileNotFoundException {
		return new FileInputStream(getFile(path));
	}

	@Override
	public Path resolve(String path) {
		if (path.contains("/")) {
			return null;
		}

		return currentRoot.resolve(path);
	}

	@Override
	public void setCurrentRoot(Path root) {
		if (!root.isAbsolute()) {
			throw new IllegalArgumentException("The root path has to be absolute!");
		}

		this.currentRoot = root;
	}

	@Override
	public boolean exists(String path) {
		return getFile(path).exists();
	}

	@Override
	public boolean isFile(String path) {
		return getFile(path).isFile();
	}

	/**
	 * Gets the file with the given path
	 * 
	 * @param path
	 *            The path of the file to be returned. If this is a relative path it
	 *            will be resolved against {@link #currentRoot}
	 * @return The file corresponding to the given path
	 */
	protected File getFile(String strPath) {
		Path path = resolve(strPath);
		if (path == null) {
			return new File("/This file doesn't exist!!\\");
		}

		return path.toFile();
	}

	@Override
	public Path getCurrentRoot() {
		return currentRoot;
	}

}
