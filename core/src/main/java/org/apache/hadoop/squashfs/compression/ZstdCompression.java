package org.apache.hadoop.squashfs.compression;

import org.apache.hadoop.squashfs.superblock.CompressionId;

import com.github.luben.zstd.Zstd;

public class ZstdCompression implements Compression
{

	private int level;

	public ZstdCompression()
	{
		this(Zstd.defaultCompressionLevel());
	}

	public ZstdCompression(int level)
	{
		this.level = level;
	}

	@Override
	public CompressionId getCompressionId()
	{
		return CompressionId.ZSTD;
	}

	public int getLevel()
	{
		return level;
	}

}
