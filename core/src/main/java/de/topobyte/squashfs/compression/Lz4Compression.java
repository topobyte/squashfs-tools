package de.topobyte.squashfs.compression;

import de.topobyte.squashfs.superblock.CompressionId;

public class Lz4Compression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.LZ4;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

}
