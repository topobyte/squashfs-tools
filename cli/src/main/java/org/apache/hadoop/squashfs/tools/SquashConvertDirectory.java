/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.squashfs.tools;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.hadoop.squashfs.SquashFsEntryBuilder;
import org.apache.hadoop.squashfs.SquashFsWriter;
import org.apache.hadoop.squashfs.compression.Compression;
import org.apache.hadoop.squashfs.compression.ZlibCompression;
import org.apache.hadoop.squashfs.util.PosixUtil;

public class SquashConvertDirectory
{

	public static void convertToSquashFs(File inputFile, File outputFile,
			Compression compression, int offset) throws IOException
	{
		System.err.printf("Converting %s -> %s...%n",
				inputFile.getAbsolutePath(), outputFile.getAbsolutePath());

		Files.deleteIfExists(outputFile.toPath());

		long fileCount = 0L;
		try (SquashFsWriter writer = new SquashFsWriter(outputFile, compression,
				offset)) {
			AtomicReference<Instant> modDate = new AtomicReference<>(
					Instant.ofEpochMilli(0));

			Path path = inputFile.toPath();
			fileCount = walk(path, path, 0, writer, modDate);

			writer.setModificationTime((int) (modDate.get().getEpochSecond()));
			writer.finish();
		}

		System.err.printf("Converted image containing %d files.%n", fileCount);
	}

	private static int walk(Path root, Path path, int depth,
			SquashFsWriter writer, AtomicReference<Instant> modDate)
			throws IOException
	{
		int count = 0;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
			for (Path file : stream) {
				if (Files.isDirectory(file)) {
					processFile(root, file, writer, modDate);
					count += walk(root, file, depth + 1, writer, modDate);
				} else {
					processFile(root, file, writer, modDate);
					count++;
				}
			}
		}
		return count;
	}

	private static void processFile(Path root, Path file, SquashFsWriter writer,
			AtomicReference<Instant> modDate) throws IOException
	{
		PosixFileAttributes posix = Files.readAttributes(file,
				PosixFileAttributes.class, NOFOLLOW_LINKS);

		int userId = (Integer) Files.getAttribute(file, "unix:uid",
				NOFOLLOW_LINKS);
		int groupId = (Integer) Files.getAttribute(file, "unix:gid",
				NOFOLLOW_LINKS);

		Path relative = root.relativize(file);

		String name = relative.toString().replaceAll("/+", "/")
				.replaceAll("^/", "").replaceAll("/$", "").replaceAll("^", "/");

		short permissions = (short) (PosixUtil
				.getPosixPermissionsAsInt(posix.permissions()) & 07777);

		System.out.println(Integer.toOctalString(permissions) + " " + name);

		Instant lastModified = Files.getLastModifiedTime(file).toInstant();
		if (lastModified.isAfter(modDate.get())) {
			modDate.set(lastModified);
		}

		SquashFsEntryBuilder tb = writer.entry(name).uid(userId).gid(groupId)
				.permissions(permissions).fileSize(Files.size(file))
				.lastModified(lastModified);

		if (Files.isSymbolicLink(file)) {
			Path symlink = Files.readSymbolicLink(file);
			System.out.println("symlink: " + symlink);
			tb.symlink(symlink.toString());
		} else if (Files.isDirectory(file)) {
			System.out.println("dir: " + file);
			tb.directory();
		} else if (Files.isRegularFile(file)) {
			tb.file();
		} else {
			throw new IOException(String.format("Unknown file type for '%s'",
					file.getFileName()));
		}

		if (Files.isRegularFile(file)) {
			tb.content(Files.newInputStream(file), Files.size(file));
		}

		tb.build();
	}

	public static void usage()
	{
		System.err.printf("Usage: %s <directory> <squashfs-file>%n",
				SquashConvertDirectory.class.getSimpleName());
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 2) {
			usage();
		}
		convertToSquashFs(new File(args[0]), new File(args[1]),
				new ZlibCompression(), 0);
	}

}
