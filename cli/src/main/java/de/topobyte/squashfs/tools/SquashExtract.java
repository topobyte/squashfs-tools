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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
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

import de.topobyte.squashfs.MappedSquashFsReader;
import de.topobyte.squashfs.SquashFsReader;
import de.topobyte.squashfs.directory.DirectoryEntry;
import de.topobyte.squashfs.inode.DirectoryINode;
import de.topobyte.squashfs.inode.FileINode;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.inode.INodeType;
import de.topobyte.squashfs.inode.SymlinkINode;
import de.topobyte.squashfs.io.MappedFile;
import de.topobyte.squashfs.metadata.MetadataReader;
import de.topobyte.squashfs.util.BinUtils;
import de.topobyte.squashfs.util.PosixUtil;

public class SquashExtract
{

	public static void usage()
	{
		System.err.printf(
				"Usage: %s [options...] <squashfs-file> <directory>%n",
				SquashExtract.class.getSimpleName());
		System.err.println();
		System.err.println("    -m,--mapped     Use mmap() for I/O");
		System.err.println("       --metadata   Dump metadata ");
		System.err
				.println("                       <file-offset> <block-offset>");
		System.err.println();
		System.exit(1);
	}

	private static SquashFsReader createReader(Path file, boolean mapped)
			throws IOException
	{
		if (mapped) {
			System.out.println("Using memory-mapped reader");
			System.out.println();
			try (RandomAccessFile raf = new RandomAccessFile(file.toFile(),
					"r")) {
				try (FileChannel channel = raf.getChannel()) {
					MappedFile mmap = MappedFile.mmap(channel,
							MappedSquashFsReader.PREFERRED_MAP_SIZE,
							MappedSquashFsReader.PREFERRED_WINDOW_SIZE, 0);

					return SquashFsReader.fromMappedFile(0, mmap);
				}
			}
		} else {
			System.out.println("Using file reader");
			System.out.println();
			return SquashFsReader.fromFile(0, file.toFile(), 0);
		}
	}

	private static void extract(SquashFsReader reader, Path directory)
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

	private static Map<Path, FileTime> directoryToLastModified = new LinkedHashMap<>();

	private static void createDirectory(DirectoryINode inode, Path file)
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

	private static void extractFileContent(SquashFsReader reader,
			FileINode inode, Path file) throws IOException
	{
		long fileSize = inode.getFileSize();
		long readSize;
		try (OutputStream fos = Files.newOutputStream(file)) {
			CountingOutputStream cos = new CountingOutputStream(fos);
			reader.writeFileStream(inode, cos);
			readSize = cos.getBytesWritten();
		}
		System.out.printf(" [%d bytes, %d read]%n", fileSize, readSize);
		Files.setLastModifiedTime(file,
				FileTime.from(inode.getModifiedTime(), TimeUnit.SECONDS));
		Files.setPosixFilePermissions(file,
				PosixUtil.getPosixPermissionsAsSet(inode.getPermissions()));
	}

	private static void createSymlink(SymlinkINode inode, Path file)
			throws IOException
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

	private static void extractSubtree(SquashFsReader reader, boolean root,
			String path, DirectoryINode inode, Path directory)
			throws IOException
	{
		System.out.printf("Decending into '%s'%n", path);
		if (root) {
			createDirectory(inode, directory);
			System.out.printf("(%d) /%n", inode.getInodeNumber());
		}

		for (DirectoryEntry entry : reader.getChildren(inode)) {
			INode childInode = reader.findInodeByDirectoryEntry(entry);
			INodeType type = childInode.getInodeType();
			String entryPath = String.format("%s%s%s", path,
					entry.getNameAsString(), type.directory() ? "/" : "");

			Path p = directory.resolve(entryPath.substring(1));

			if (type.directory()) {
				System.out.printf("(%d) Creating directory '%s'%n",
						childInode.getInodeNumber(), p);
				createDirectory((DirectoryINode) childInode, p);
			} else if (type.file()) {
				System.out.printf("(%d) Extracting file '%s'",
						childInode.getInodeNumber(), p);
				extractFileContent(reader, (FileINode) childInode, p);
			} else if (type.symlink()) {
				System.out.printf("(%d) Creating symlink '%s'%n",
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

	private static void dumpMetadataBlock(SquashFsReader reader,
			long metaFileOffset, int metaBlockOffset) throws IOException
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

	public static void main(String[] args) throws Exception
	{
		boolean mapped = false;
		boolean metadata = false;
		long metaFileOffset = 0L;
		int metaBlockOffset = 0;

		String squashfs = null;
		String dir = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
			case "-m":
			case "--mapped":
				mapped = true;
				break;
			case "--metadata":
				metadata = true;
				if (i + 2 >= args.length) {
					usage();
				}
				metaFileOffset = Long.parseLong(args[++i], 10);
				metaBlockOffset = Integer.parseInt(args[++i], 10);
				break;
			default:
				if (squashfs != null && dir != null) {
					usage();
				}
				if (squashfs == null) {
					squashfs = arg;
				} else if (dir == null) {
					dir = arg;
				}
			}
		}
		if (squashfs == null || dir == null) {
			usage();
		}

		Path directory = Paths.get(dir);
		if (Files.exists(directory)) {
			System.out.printf("Output directory '%s' exists. Exit%n", dir);
			System.exit(1);
		}

		try (SquashFsReader reader = createReader(Paths.get(squashfs),
				mapped)) {
			System.out.println(reader.getSuperBlock());
			System.out.println();
			System.out.println(reader.getIdTable());
			System.out.println();
			System.out.println(reader.getFragmentTable());
			System.out.println();
			System.out.println(reader.getExportTable());
			System.out.println();

			extract(reader, directory);

			if (metadata) {
				dumpMetadataBlock(reader, metaFileOffset, metaBlockOffset);
			}
		}
	}

}
