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

package de.topobyte.squashfs.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.topobyte.squashfs.SquashFsReader;
import de.topobyte.squashfs.directory.DirectoryEntry;
import de.topobyte.squashfs.inode.DirectoryINode;
import de.topobyte.squashfs.inode.FileINode;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.metadata.MetadataReader;
import de.topobyte.squashfs.util.BinUtils;

public class SquashFsck
{

	public void dumpTree(SquashFsReader reader, boolean readFiles)
			throws IOException
	{
		System.out.println("Directory tree:");
		System.out.println();
		DirectoryINode root = reader.getRootInode();
		dumpSubtree(reader, true, "/", root, readFiles);
	}

	private void dumpFileContent(SquashFsReader reader, FileINode inode)
			throws IOException
	{
		long fileSize = inode.getFileSize();
		long readSize;
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			reader.writeFileStream(inode, bos);
			byte[] content = bos.toByteArray();
			readSize = content.length;
		}
		System.out.printf("  %d bytes, %d read%n", fileSize, readSize);
	}

	private void dumpSubtree(SquashFsReader reader, boolean root, String path,
			DirectoryINode inode, boolean readFiles) throws IOException
	{
		if (root) {
			System.out.printf("/ (%d)%n", inode.getInodeNumber());
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			System.out.printf("%s%s%s (%d)%n", path, entry.getNameAsString(),
					childInode.getInodeType().directory() ? "/" : "",
					childInode.getInodeNumber());

			if (readFiles && childInode.getInodeType().file()) {
				dumpFileContent(reader, (FileINode) childInode);
			}
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			if (childInode.getInodeType().directory()) {
				dumpSubtree(reader, false,
						String.format("%s%s/", path, entry.getNameAsString()),
						(DirectoryINode) childInode, readFiles);
			}
		}
	}

	public void dumpMetadataBlock(SquashFsReader reader, long metaFileOffset,
			int metaBlockOffset) throws IOException
	{
		System.out.println();
		System.out.printf("Dumping block at file offset %d, block offset %d%n",
				metaFileOffset, metaBlockOffset);
		System.out.println();

		MetadataReader mr = reader.getMetaReader().rawReader(0, metaFileOffset,
				(short) metaBlockOffset);
		mr.isEof(); // make sure block is read
		byte[] buf = new byte[mr.available()];
		mr.readFully(buf);

		StringBuilder sb = new StringBuilder();
		BinUtils.dumpBin(sb, 0, "data", buf, 0, buf.length, 32, 2);
		System.out.println(sb.toString());
	}

}
