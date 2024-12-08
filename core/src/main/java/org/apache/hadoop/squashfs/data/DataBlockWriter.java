/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.squashfs.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.apache.hadoop.squashfs.CompressionUtil;
import org.apache.hadoop.squashfs.ra.IRandomAccess;
import org.apache.hadoop.squashfs.superblock.CompressionId;

public class DataBlockWriter
{

	private final IRandomAccess raf;
	private final int blockSize;
	private final CompressionId compression;

	public DataBlockWriter(IRandomAccess raf, int blockSize,
			CompressionId compression)
	{
		this.raf = raf;
		this.blockSize = blockSize;
		this.compression = compression;
	}

	public DataBlockRef write(byte[] data, int offset, int length)
			throws IOException
	{
		if (length != blockSize) {
			throw new IllegalArgumentException(
					String.format("Invalid block length %d (expected %d)",
							length, blockSize));
		}

		long fileOffset = raf.getFilePointer();

		if (isSparse(data, offset, length)) {
			return new DataBlockRef(fileOffset, length, 0, false, true);
		}

		byte[] compressed = null;
		switch (compression) {
		case NONE:
		case LZ4:
		case LZMA:
		case LZO:
		case XZ:
			break;
		case ZLIB:
			compressed = compressZlib(data, offset, length);
			break;
		case ZSTD:
			compressed = compressZstd(data, offset, length);
			break;
		}
		if (compressed != null) {
			raf.write(compressed);
			return new DataBlockRef(fileOffset, length, compressed.length, true,
					false);
		}

		raf.write(data, offset, length);
		return new DataBlockRef(fileOffset, length, length, false, false);
	}

	private boolean isSparse(byte[] data, int offset, int length)
	{
		int end = offset + length;
		for (int i = offset; i < end; i++) {
			if (data[i] != 0) {
				return false;
			}
		}
		return true;
	}

	private byte[] compressZlib(byte[] data, int offset, int length)
			throws IOException
	{
		Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (DeflaterOutputStream dos = new DeflaterOutputStream(bos, def,
					4096)) {
				dos.write(data, offset, length);
			}
			byte[] result = bos.toByteArray();
			if (result.length > blockSize) {
				return null;
			}
			return result;
		} finally {
			def.end();
		}
	}

	private byte[] compressZstd(byte[] data, int offset, int length)
			throws IOException
	{
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (OutputStream zos = CompressionUtil
					.createZstdOutputStream(bos)) {
				zos.write(data, offset, length);
			}
			byte[] result = bos.toByteArray();
			if (result.length > blockSize) {
				return null;
			}
			return result;
		}
	}

}
