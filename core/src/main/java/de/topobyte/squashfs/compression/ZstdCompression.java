package de.topobyte.squashfs.compression;

import com.github.luben.zstd.Zstd;

import de.topobyte.squashfs.superblock.CompressionId;

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

	@Override
	public String toString()
	{
		return getClass().getSimpleName();
	}

}
