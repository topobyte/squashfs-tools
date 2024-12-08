package org.apache.hadoop.squashfs.compression;

import org.apache.hadoop.squashfs.superblock.CompressionId;

public interface Compression
{

	CompressionId getCompressionId();

}
