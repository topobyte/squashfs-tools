package org.apache.hadoop.squashfs.compression;

import org.apache.hadoop.squashfs.superblock.CompressionId;

public class Lz4Compression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.LZO;
	}

}
