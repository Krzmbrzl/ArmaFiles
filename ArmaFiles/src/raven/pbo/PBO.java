package raven.pbo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import raven.misc.ByteReader;

/**
 * A class representing a PBO-file
 * 
 * @author Raven
 *
 */
public class PBO {

	/**
	 * The location of this PBO
	 */
	protected File rootFile;
	/**
	 * The list of entries in this PBO
	 */
	protected List<PBOEntry> entries;
	/**
	 * The offset at which the actual content of the PBO starts (After the header)
	 */
	protected int contentStart;
	/**
	 * The header extension for this PBO
	 */
	protected String[] headerExtension;


	/**
	 * Creates a new PBO object from the given file
	 * 
	 * @param file
	 *            The {@linkplain FIle} pointing to the PBO on the hard drive. This
	 *            has to exist
	 * @throws IOException
	 */
	public PBO(File file) throws IOException {
		validate(file);
		this.rootFile = file;
		entries = new ArrayList<>();

		readHeader();
	}

	/**
	 * Validates that the given file is applicable for constructing a PBO object
	 * from it
	 * 
	 * @param file
	 *            The {@linkplain File} to validate
	 */
	protected void validate(File file) {
		if (file == null) {
			throw new IllegalArgumentException("The given file must not be null!");
		}
		if (!file.exists()) {
			throw new IllegalArgumentException("The given file has to exist!");
		}
		if (!file.isFile()) {
			throw new IllegalArgumentException("The given file is not actually a file!");
		}
		if (!file.getName().toLowerCase().endsWith(".pbo")) {
			throw new IllegalArgumentException("The given file does not have the .pbo extentsion!");
		}
	}

	/**
	 * Gets the location of this PBO on the hard drive
	 */
	public Path getLocation() {
		return rootFile.toPath();
	}

	/**
	 * Gets a {@linkplain File} pointing to the PBO's location on the hard drive
	 */
	public File toFile() {
		return new File(rootFile.toURI());
	}

	/**
	 * Reads the PBO header and populates {@link #entries}
	 * 
	 * @throws IOException
	 */
	protected void readHeader() throws IOException {
		ByteReader reader = new ByteReader(new FileInputStream(rootFile));

		int relativeStartOffsetOffset = 0;

		// first entry -> may be followed by header extension
		PBOEntry entry = new PBOEntry(reader, relativeStartOffsetOffset, this);

		entries.add(entry);

		if (entry.isBoundary()) {
			// there is a header extension present
			String s;
			List<String> extensionEntries = new ArrayList<>();

			while (!(s = reader.readString()).isEmpty()) {
				extensionEntries.add(s);
			}

			headerExtension = extensionEntries.toArray(new String[extensionEntries.size()]);
		}

		relativeStartOffsetOffset += entry.getDataSize();

		do {
			entry = new PBOEntry(reader, relativeStartOffsetOffset, this);
			relativeStartOffsetOffset += entry.getDataSize();
			entries.add(entry);
		} while (!entry.getFileName().isEmpty());

		contentStart = reader.getPosition();

		reader.close();
	}

	/**
	 * Gets a list of the {@linkplain PBOEntry} representing a file contained in
	 * this PBO
	 */
	public List<PBOEntry> getEntries() {
		if (entries.size() == 0) {
			return new ArrayList<>();
		}

		// don't return boundary entries
		return new ArrayList<>(entries.subList(1, entries.size() - 2));
	}

	/**
	 * Gets a list of all {@linkplain PBOEntry} contained in this PBO (including
	 * boundary entries)
	 */
	public List<PBOEntry> getAllEntries() {
		return new ArrayList<>(entries);
	}

	/**
	 * Gets the offset in this file at which the actual content begins. This is the
	 * offset all {@linkplain PBOEntry#getStartOffset()} are relative to
	 */
	public int getContentOffset() {
		return contentStart;
	}

	/**
	 * Gets the header extension of this PBO as an array in which each entry
	 * specifies a separate entry in the header extension. If there is no header
	 * extension this method returns <code>null</code>
	 */
	public String[] getHeaderExtension() {
		return headerExtension == null ? null : Arrays.copyOf(headerExtension, headerExtension.length);
	}

	/**
	 * Tries to find the {@linkplain PBOEntry} with the given name
	 * 
	 * @param name
	 *            The name to search for
	 * @return The respective entry or <code>null</code> if none could be found
	 */
	public PBOEntry getEntry(String name) {
		name = name.toLowerCase();
		for (PBOEntry current : entries) {
			if (current.getFileName().toLowerCase().equals(name)) {
				return current;
			}
		}

		return null;
	}

	/**
	 * Gets the prefix for this PBO or <code>null</code> if there is none
	 */
	public String getPrefix() {
		if (headerExtension == null || headerExtension.length < 2) {
			return null;
		}

		for (int i = 0; i < headerExtension.length - 1; i++) {
			String current = headerExtension[i];

			if (current == null) {
				return null;
			}

			if (current.toLowerCase().equals("prefix")) {
				return headerExtension[i + 1];
			}
		}

		return null;
	}

}
