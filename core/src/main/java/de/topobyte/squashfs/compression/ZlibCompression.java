package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class ZlibCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.ZLIB;
	}

}
