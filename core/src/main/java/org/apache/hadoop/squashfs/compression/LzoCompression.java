package org.apache.hadoop.squashfs.compression;

import org.apache.hadoop.squashfs.superblock.CompressionId;

public class LzoCompression implements Compression
{

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.LZ4;
	}

}
