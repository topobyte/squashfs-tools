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

package de.topobyte.squashfs.tools;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.SquashFsEntryBuilder;
import de.topobyte.squashfs.SquashFsWriter;
import de.topobyte.squashfs.compression.Compression;
import de.topobyte.squashfs.util.PosixUtil;

public class SquashConvertDirectory
{

	final static Logger logger = LoggerFactory
			.getLogger(SquashConvertDirectory.class);

	public void convertToSquashFs(Path inputFile, Path outputFile,
			Compression compression, int offset) throws IOException
	{
		logger.info("Converting {} -> {}...", inputFile.toAbsolutePath(),
				outputFile.toAbsolutePath());

		Files.deleteIfExists(outputFile);

		long fileCount = 0L;
		try (SquashFsWriter writer = new SquashFsWriter(outputFile.toFile(),
				compression, offset)) {
			AtomicReference<Instant> modDate = new AtomicReference<>(
					Instant.ofEpochMilli(0));

			fileCount = walk(inputFile, inputFile, 0, writer, modDate);

			writer.setModificationTime((int) (modDate.get().getEpochSecond()));
			writer.finish();
		}

		logger.info("Converted image containing {} files.", fileCount);
	}

	private int walk(Path root, Path path, int depth, SquashFsWriter writer,
			AtomicReference<Instant> modDate) throws IOException
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

	private void processFile(Path root, Path file, SquashFsWriter writer,
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

		logger.info(Integer.toOctalString(permissions) + " " + name);

		Instant lastModified = Files.getLastModifiedTime(file).toInstant();
		if (lastModified.isAfter(modDate.get())) {
			modDate.set(lastModified);
		}

		SquashFsEntryBuilder tb = writer.entry(name).uid(userId).gid(groupId)
				.permissions(permissions).fileSize(Files.size(file))
				.lastModified(lastModified);

		if (Files.isSymbolicLink(file)) {
			Path symlink = Files.readSymbolicLink(file);
			logger.info("symlink: " + symlink);
			tb.symlink(symlink.toString());
		} else if (Files.isDirectory(file)) {
			logger.info("dir: " + file);
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

}
