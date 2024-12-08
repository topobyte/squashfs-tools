package org.apache.hadoop.squashfs;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.squashfs.compression.Compression;
import org.apache.hadoop.squashfs.compression.Lz4Compression;
import org.apache.hadoop.squashfs.compression.LzmaCompression;
import org.apache.hadoop.squashfs.compression.LzoCompression;
import org.apache.hadoop.squashfs.compression.NoCompression;
import org.apache.hadoop.squashfs.compression.XzCompression;
import org.apache.hadoop.squashfs.compression.ZlibCompression;
import org.apache.hadoop.squashfs.compression.ZstdCompression;
import org.apache.hadoop.squashfs.superblock.CompressionId;

import com.github.luben.zstd.ZstdOutputStream;

public class CompressionUtil
{

	public static ZstdOutputStream createZstdOutputStream(OutputStream os,
			ZstdCompression options) throws IOException
	{
		return new ZstdOutputStream(os, options.getLevel());
	}

	public static Compression fromCompressionId(CompressionId compressionId)
	{
		switch (compressionId) {
		default:
		case NONE:
			return new NoCompression();
		case ZLIB:
			return new ZlibCompression();
		case ZSTD:
			return new ZstdCompression();
		case LZO:
			return new LzoCompression();
		case XZ:
			return new XzCompression();
		case LZ4:
			return new Lz4Compression();
		case LZMA:
			return new LzmaCompression();
		}
	}

}
