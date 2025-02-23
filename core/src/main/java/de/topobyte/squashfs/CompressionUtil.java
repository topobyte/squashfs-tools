package de.topobyte.squashfs;

import java.io.IOException;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdOutputStream;

import de.topobyte.squashfs.compression.Compression;
import de.topobyte.squashfs.compression.Lz4Compression;
import de.topobyte.squashfs.compression.LzmaCompression;
import de.topobyte.squashfs.compression.LzoCompression;
import de.topobyte.squashfs.compression.NoCompression;
import de.topobyte.squashfs.compression.XzCompression;
import de.topobyte.squashfs.compression.ZlibCompression;
import de.topobyte.squashfs.compression.ZstdCompression;
import de.topobyte.squashfs.superblock.CompressionId;

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
