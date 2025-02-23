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

package de.topobyte.squashfs.test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import de.topobyte.squashfs.compression.ZlibCompression;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.metadata.MemoryMetadataBlockReader;
import de.topobyte.squashfs.metadata.MetadataBlockReader;
import de.topobyte.squashfs.metadata.MetadataReader;
import de.topobyte.squashfs.metadata.MetadataWriter;
import de.topobyte.squashfs.superblock.SuperBlock;
import de.topobyte.squashfs.util.BinUtils;

public class INodeTestUtils
{

	public static byte[] serializeINode(INode inode) throws IOException
	{
		MetadataWriter writer = new MetadataWriter(new ZlibCompression());
		inode.writeData(writer);

		byte[] data;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try (DataOutputStream dos = new DataOutputStream(bos)) {
				writer.save(dos);
			}
			data = bos.toByteArray();

			StringBuilder buf = new StringBuilder();
			BinUtils.dumpBin(buf, 15, "serialized-data", data, 0,
					Math.min(256, data.length), 16, 2);
			System.out.println(buf.toString());
		}

		return data;
	}

	public static INode deserializeINode(byte[] data) throws IOException
	{
		SuperBlock sb = new SuperBlock();
		sb.setCompression(new ZlibCompression());
		sb.setBlockSize(131072);
		sb.setBlockLog((short) 17);
		sb.setVersionMajor((short) 4);
		sb.setVersionMinor((short) 0);

		int tag = 0;
		try (MetadataBlockReader mbr = new MemoryMetadataBlockReader(tag, sb,
				data)) {
			MetadataReader reader = mbr.rawReader(tag, 0L, (short) 0);

			return INode.read(sb, reader);
		}
	}
}
