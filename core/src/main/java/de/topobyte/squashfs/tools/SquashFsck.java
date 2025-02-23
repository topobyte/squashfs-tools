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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.SquashFsReader;
import de.topobyte.squashfs.directory.DirectoryEntry;
import de.topobyte.squashfs.inode.DirectoryINode;
import de.topobyte.squashfs.inode.FileINode;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.metadata.MetadataReader;
import de.topobyte.squashfs.util.BinUtils;

public class SquashFsck
{

	final static Logger logger = LoggerFactory.getLogger(SquashFsck.class);

	public void dumpTree(SquashFsReader reader, boolean readFiles)
			throws IOException
	{
		logger.info("Directory tree:");
		logger.info("");
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
		logger.info("  {} bytes, {} read", fileSize, readSize);
	}

	private void dumpSubtree(SquashFsReader reader, boolean root, String path,
			DirectoryINode inode, boolean readFiles) throws IOException
	{
		if (root) {
			logger.info("/ ({})", inode.getInodeNumber());
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			logger.info("{}{}{} ({})", path, entry.getNameAsString(),
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
		logger.info("");
		logger.info("Dumping block at file offset {}, block offset {}",
				metaFileOffset, metaBlockOffset);
		logger.info("");

		MetadataReader mr = reader.getMetaReader().rawReader(0, metaFileOffset,
				(short) metaBlockOffset);
		mr.isEof(); // make sure block is read
		byte[] buf = new byte[mr.available()];
		mr.readFully(buf);

		StringBuilder sb = new StringBuilder();
		BinUtils.dumpBin(sb, 0, "data", buf, 0, buf.length, 32, 2);
		logger.info(sb.toString());
	}

}
