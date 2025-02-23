package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public interface Compression
{

	CompressionId getCompressionId();

}
