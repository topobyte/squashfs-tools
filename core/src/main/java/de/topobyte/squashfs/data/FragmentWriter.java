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

package de.topobyte.squashfs.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import de.topobyte.squashfs.CompressionUtil;
import de.topobyte.squashfs.compression.Compression;
import de.topobyte.squashfs.compression.ZstdCompression;
import de.topobyte.squashfs.metadata.MetadataBlockRef;
import de.topobyte.squashfs.metadata.MetadataWriter;
import de.topobyte.squashfs.ra.IRandomAccess;
import de.topobyte.squashfs.table.FragmentTable;
import de.topobyte.squashfs.table.FragmentTableEntry;

public class FragmentWriter
{

	private final IRandomAccess raf;
	private final int blockSize;
	private final Compression compression;
	private final byte[] currentBlock;
	private final List<FragmentRef> currentFragments = new ArrayList<>();
	private int currentOffset = 0;
	private final List<FragmentTableEntry> fragmentEntries = new ArrayList<>();

	public FragmentWriter(IRandomAccess raf, int blockSize,
			Compression compression)
	{
		this.raf = raf;
		this.blockSize = blockSize;
		this.compression = compression;
		this.currentBlock = new byte[blockSize];
	}

	public FragmentRef write(byte[] data, int offset, int length)
			throws IOException
	{
		if (length > blockSize || length <= 0) {
			throw new IllegalArgumentException(
					String.format("Invalid fragment length %d (min 1, max %d)",
							length, blockSize));
		}

		if (currentOffset + length > currentBlock.length) {
			flush();
		}

		System.arraycopy(data, offset, currentBlock, currentOffset, length);

		FragmentRef frag = new FragmentRef(currentOffset);
		currentFragments.add(frag);
		currentOffset += length;
		return frag;
	}

	public List<FragmentTableEntry> getFragmentEntries()
	{
		return fragmentEntries;
	}

	public List<MetadataBlockRef> save(MetadataWriter writer) throws IOException
	{

		List<MetadataBlockRef> fragmentRefs = new ArrayList<>();

		for (int i = 0; i < fragmentEntries.size(); i++) {
			FragmentTableEntry fragment = fragmentEntries.get(i);

			if (i % FragmentTable.ENTRIES_PER_BLOCK == 0) {
				fragmentRefs.add(writer.getCurrentReference());
			}

			writer.writeLong(fragment.getStart());
			writer.writeInt(fragment.getSize());
			writer.writeInt(0); // placeholder
		}

		return fragmentRefs;
	}

	public int getFragmentEntryCount()
	{
		return fragmentEntries.size();
	}

	public int getFragmentTableRefSize()
	{
		int entryCount = fragmentEntries.size();
		return (entryCount / FragmentTable.ENTRIES_PER_BLOCK)
				+ ((entryCount % FragmentTable.ENTRIES_PER_BLOCK == 0) ? 0 : 1);
	}

	public void flush() throws IOException
	{
		long fileOffset = raf.getFilePointer();

		byte[] compressed = null;
		int size = 0;
		if (currentOffset <= 0) {
			return;
		}

		compressed = compressData();
		if (compressed == null) {
			raf.write(currentBlock, 0, currentOffset);
			size = currentOffset;
		} else {
			raf.write(compressed);
			size = compressed.length;
		}

		FragmentTableEntry fragEntry = new FragmentTableEntry(fileOffset, size,
				compressed != null);
		fragmentEntries.add(fragEntry);

		for (FragmentRef frag : currentFragments) {
			frag.commit(fragmentEntries.size() - 1);
		}

		currentFragments.clear();
		currentOffset = 0;
	}

	private byte[] compressData() throws IOException
	{
		switch (compression.getCompressionId()) {
		case ZLIB:
			return compressDataZlib();
		case ZSTD:
			return compressDataZstd((ZstdCompression) compression);
		case LZ4:
		case LZMA:
		case LZO:
		case NONE:
		case XZ:
		default:
			return null;
		}
	}

	private byte[] compressDataZlib() throws IOException
	{
		Deflater def = new Deflater(Deflater.BEST_COMPRESSION);
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (DeflaterOutputStream dos = new DeflaterOutputStream(bos, def,
					4096)) {
				dos.write(currentBlock, 0, currentOffset);
			}
			byte[] result = bos.toByteArray();
			if (result.length > currentOffset) {
				return null;
			}
			return result;
		} finally {
			def.end();
		}
	}

	private byte[] compressDataZstd(ZstdCompression options) throws IOException
	{
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (OutputStream zos = CompressionUtil.createZstdOutputStream(bos,
					options)) {
				zos.write(currentBlock, 0, currentOffset);
			}
			byte[] result = bos.toByteArray();
			if (result.length > currentOffset) {
				return null;
			}
			return result;
		}
	}

}
