package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class LzoCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.LZO;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

}
