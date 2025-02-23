package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class XzCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.XZ;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

}
