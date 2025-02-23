package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class LzmaCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.LZMA;
	}

}
