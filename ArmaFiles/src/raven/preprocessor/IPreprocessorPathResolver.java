package raven.preprocessor;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * An interface describing an option that can resolve the paths given in an
 * include-statement during preprocessing
 * 
 * @author Raven
 *
 */
public interface IPreprocessorPathResolver {

	/**
	 * Resolves the given path and returns an {@linkplain InputStream} to it
	 * 
	 * @param path
	 *            The path to be resolved. May be either absolute or relative
	 * @return An {@linkplain InputStream} to the resolved resource
	 * @throws FileNotFoundException
	 *             If the given path couldn't be resolved
	 */
	public InputStream getStream(String path) throws FileNotFoundException;

	/**
	 * Resolves the given path.
	 * 
	 * @param path
	 *            The path to be resolved. May be either absolute or relative
	 * @return The resolved {@linkplain Path} or<code>null</code> if the path
	 *         couldn't be resolved
	 */
	public Path resolve(String path);

	/**
	 * Sets the current working directory (Path: ./)
	 * 
	 * @param workingDir
	 *            The {@linkplain Path} corresponding to "./"
	 */
	public void setCurrentRoot(Path root);

	/**
	 * Checks if the file corresponding to the given path exists.
	 * 
	 * @param path
	 *            The path to check
	 */
	public boolean exists(String path);

	/**
	 * Checks whether the file corresponding to the given path is actually a file.
	 * 
	 * @param path
	 *            The path to check
	 * @return <code>True</code> if the given path corresponds to an <i>existing</i>
	 *         file
	 */
	public boolean isFile(String path);

	/**
	 * Gets the current root of this resolver
	 */
	public Path getCurrentRoot();
}
