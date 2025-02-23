package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class NoCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.NONE;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

}
