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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.utils.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.SquashFsReader;
import de.topobyte.squashfs.directory.DirectoryEntry;
import de.topobyte.squashfs.inode.DirectoryINode;
import de.topobyte.squashfs.inode.FileINode;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.inode.INodeType;
import de.topobyte.squashfs.inode.SymlinkINode;
import de.topobyte.squashfs.metadata.MetadataReader;
import de.topobyte.squashfs.util.BinUtils;
import de.topobyte.squashfs.util.PosixUtil;

public class SquashExtract
{

	final static Logger logger = LoggerFactory.getLogger(SquashExtract.class);

	public void extract(SquashFsReader reader, Path directory)
			throws IOException
	{
		DirectoryINode root = reader.getRootInode();
		extractSubtree(reader, true, "/", root, directory);

		// Creating files within the previously created directories modifies the
		// contained directory's last modified time, so we need to update it
		// again for each directory.
		for (Entry<Path, FileTime> entry : directoryToLastModified.entrySet()) {
			Files.setLastModifiedTime(entry.getKey(), entry.getValue());
		}
	}

	private Map<Path, FileTime> directoryToLastModified = new LinkedHashMap<>();

	private void createDirectory(DirectoryINode inode, Path file)
			throws IOException
	{
		Files.createDirectories(file);
		FileTime fileTime = FileTime.from(inode.getModifiedTime(),
				TimeUnit.SECONDS);
		Files.setLastModifiedTime(file, fileTime);
		Files.setPosixFilePermissions(file,
				PosixUtil.getPosixPermissionsAsSet(inode.getPermissions()));

		directoryToLastModified.put(file, fileTime);
	}

	private void extractFileContent(SquashFsReader reader, FileINode inode,
			Path file) throws IOException
	{
		long fileSize = inode.getFileSize();
		long readSize;
		try (OutputStream fos = Files.newOutputStream(file)) {
			CountingOutputStream cos = new CountingOutputStream(fos);
			reader.writeFileStream(inode, cos);
			readSize = cos.getBytesWritten();
		}
		logger.info("[File has {} bytes, {} read]", fileSize, readSize);
		Files.setLastModifiedTime(file,
				FileTime.from(inode.getModifiedTime(), TimeUnit.SECONDS));
		Files.setPosixFilePermissions(file,
				PosixUtil.getPosixPermissionsAsSet(inode.getPermissions()));
	}

	private void createSymlink(SymlinkINode inode, Path file) throws IOException
	{
		String target = new String(inode.getTargetPath(),
				StandardCharsets.ISO_8859_1);
		Path targetPath = Paths.get(target);
		Files.createSymbolicLink(file, targetPath);

		// Use more complicated means to set last modified time here in order to
		// specify option to not follow links
		BasicFileAttributeView attributes = Files.getFileAttributeView(file,
				BasicFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
		attributes.setTimes(
				FileTime.from(inode.getModifiedTime(), TimeUnit.SECONDS), null,
				null);

		// Cannot set permissions on symbolic links (at least on Linux)
	}

	private void extractSubtree(SquashFsReader reader, boolean root,
			String path, DirectoryINode inode, Path directory)
			throws IOException
	{
		logger.info("Decending into '{}'", path);
		if (root) {
			createDirectory(inode, directory);
			logger.info("({}) /", inode.getInodeNumber());
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			INodeType type = childInode.getInodeType();
			String entryPath = String.format("%s%s%s", path,
					entry.getNameAsString(), type.directory() ? "/" : "");

			Path p = directory.resolve(entryPath.substring(1));

			if (type.directory()) {
				logger.info("({}) Creating directory '{}'",
						childInode.getInodeNumber(), p);
				createDirectory((DirectoryINode) childInode, p);
			} else if (type.file()) {
				logger.info("({}) Extracting file '{}'",
						childInode.getInodeNumber(), p);
				extractFileContent(reader, (FileINode) childInode, p);
			} else if (type.symlink()) {
				logger.info("({}) Creating symlink '{}'",
						childInode.getInodeNumber(), p);
				createSymlink((SymlinkINode) childInode, p);
			}
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			if (childInode.getInodeType().directory()) {
				extractSubtree(reader, false,
						String.format("%s%s/", path, entry.getNameAsString()),
						(DirectoryINode) childInode, directory);
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
		logger.info(System.lineSeparator() + sb);
	}

}
