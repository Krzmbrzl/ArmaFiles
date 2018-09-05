package raven.pbo;

import java.io.IOException;

import raven.misc.ByteReader;

/**
 * A class representing an entry inside a PBO-header, describing a file
 * contained in the PBO (or a boundary file).
 * 
 * @author Raven
 *
 */
public class PBOEntry {

	public static final int UNCOMPRESSED = 0x00000000;
	public static final int COMPRESSED = 0x43707273;
	public static final int PRODUCT_ENTRY = 0x56657273;

	/**
	 * The filename of the file this entry represents
	 */
	protected String fileName;
	/**
	 * Whether the referenced file is compressed
	 */
	protected int packingMethod;
	/**
	 * The uncompressed size of the referenced file
	 */
	protected int originalSize;
	/**
	 * 
	 */
	protected int reserved;
	/**
	 * A time stamp in unix time
	 */
	protected int timeStamp;
	/**
	 * The actual size of the referenced file
	 */
	protected int dataSize;
	/**
	 * The starting offset of the referenced file relative to the end of the PBO's
	 * header
	 */
	protected int relativeStartOffset;
	/**
	 * The {@linkplain PBO} this entry belongs to
	 */
	protected PBO pbo;


	/**
	 * Creates a new entry and populating it using the provided reader
	 * 
	 * @param reader
	 *            The {@linkplain ByteReader} to use for reading the respective data
	 *            out of the stream
	 * @param relativeStartOffset
	 *            The relative start offset the content of the file represented by
	 *            this entry begins inside the PBO-file
	 * @param pbo
	 *            The {@linkplain PBO} this entry belongs to
	 * @throws IOException
	 */
	public PBOEntry(ByteReader reader, int relativeStartOffset, PBO pbo) throws IOException {
		fileName = reader.readString();

		packingMethod = reader.readInt32();
		originalSize = reader.readInt32();
		reserved = reader.readInt32();
		timeStamp = reader.readInt32();
		dataSize = reader.readInt32();

		this.relativeStartOffset = relativeStartOffset;
		this.pbo = pbo;
	}

	/**
	 * Gets the name of the file represented by this entry. May be empty if this is
	 * a boundary entry
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Gets the specified compression method for the file represented by this entry.
	 * This can either be {@link #UNCOMPRESSED} or {@link #COMPRESSED}. If this is a
	 * boundary entry this might also be {@link #PRODUCT_ENTRY}
	 */
	public int getCompressionMethod() {
		return packingMethod;
	}

	/**
	 * Gets the uncompressed data size of the file represented by this entry
	 */
	public int getOriginalSize() {
		return originalSize == 0 && !isCompressed() ? getDataSize() : originalSize;
	}

	public int getReserved() {
		return reserved;
	}

	/**
	 * Gets the timestamp as specified for this entry
	 */
	public int getTimeStamp() {
		return timeStamp;
	}

	/**
	 * Gets the size of the file corresponding to this entry inside the PBO-itself
	 */
	public int getDataSize() {
		return dataSize;
	}

	/**
	 * Gets the offset at which the content of the file corresponding to this entry
	 * starts inside the PBO
	 */
	public int getStartOffset() {
		return relativeStartOffset + pbo.getContentOffset();
	}

	/**
	 * Gets the offset at which the content of the file corresponding to this entry
	 * starts inside the PBO relative to the end of the PBO header
	 */
	public int getRelativeStartOffset() {
		return relativeStartOffset;
	}

	/**
	 * Detects if this entry is a boundary entry (empty filename)
	 */
	public boolean isBoundary() {
		return fileName.isEmpty();
	}

	/**
	 * Detects whether the file corresponding to this entry is compressed
	 */
	public boolean isCompressed() {
		return getCompressionMethod() == COMPRESSED;
	}

	@Override
	public String toString() {
		return (isBoundary() ? "$boundary entry$" : fileName);
	}

	/**
	 * Gets the {@linkplain PBO} this entry belongs to
	 */
	public PBO getPBO() {
		return pbo;
	}

	/**
	 * Creates a new {@linkplain PBOInputStream} that will be able to read the
	 * content of the file represented by this entry
	 * 
	 * @return The created stream
	 * @throws IOException
	 *             If there are errors reading the stream
	 * @throws IllegalStateException
	 *             If this entry is a boundary entry
	 */
	public PBOInputStream toStream() throws IOException {
		return new PBOInputStream(this);
	}
}
