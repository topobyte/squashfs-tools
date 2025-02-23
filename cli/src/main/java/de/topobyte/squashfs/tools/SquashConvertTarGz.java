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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import de.topobyte.squashfs.SquashFsEntryBuilder;
import de.topobyte.squashfs.SquashFsWriter;
import de.topobyte.squashfs.compression.Compression;
import de.topobyte.squashfs.compression.ZlibCompression;
import de.topobyte.squashfs.util.SizeTrackingInputStream;

public class SquashConvertTarGz
{

	public static void convertToSquashFs(Path inputFile, Path outputFile,
			Compression compression, int offset) throws IOException
	{
		System.err.printf("Converting %s -> %s...%n",
				inputFile.toAbsolutePath(), outputFile.toAbsolutePath());

		Files.deleteIfExists(outputFile);

		try (InputStream fis = Files.newInputStream(inputFile);
				SizeTrackingInputStream stis = new SizeTrackingInputStream(fis);
				GZIPInputStream gis = new GZIPInputStream(stis);
				TarArchiveInputStream tis = new TarArchiveInputStream(gis)) {

			long fileCount = 0L;
			try (SquashFsWriter writer = new SquashFsWriter(outputFile.toFile(),
					compression, offset)) {
				TarArchiveEntry entry;
				AtomicReference<Date> modDate = new AtomicReference<>(
						new Date(0));

				while ((entry = tis.getNextTarEntry()) != null) {
					processTarEntry(stis, tis, entry, writer, modDate);
					fileCount++;
				}
				writer.setModificationTime(
						(int) (modDate.get().getTime() / 1000L));
				writer.finish();
			}

			System.err.printf("Converted image containing %d files.%n",
					fileCount);
		}
	}

	private static void processTarEntry(SizeTrackingInputStream stis,
			TarArchiveInputStream tis, TarArchiveEntry entry,
			SquashFsWriter writer, AtomicReference<Date> modDate)
			throws IOException
	{
		int userId = (int) entry.getLongUserId();
		int groupId = (int) entry.getLongGroupId();

		String name = entry.getName().replaceAll("/+", "/").replaceAll("^/", "")
				.replaceAll("/$", "").replaceAll("^", "/");

		System.err.println(name);

		short permissions = (short) (entry.getMode() & 07777);

		Date lastModified = entry.getLastModifiedDate();
		if (lastModified.after(modDate.get())) {
			modDate.set(lastModified);
		}

		SquashFsEntryBuilder tb = writer.entry(name).uid(userId).gid(groupId)
				.permissions(permissions).fileSize(entry.getSize())
				.lastModified(lastModified);

		if (entry.isSymbolicLink()) {
			tb.symlink(entry.getLinkName());
		} else if (entry.isDirectory()) {
			tb.directory();
		} else if (entry.isFile()) {
			tb.file();
		} else if (entry.isBlockDevice()) {
			tb.blockDev(entry.getDevMajor(), entry.getDevMinor());
		} else if (entry.isCharacterDevice()) {
			tb.charDev(entry.getDevMajor(), entry.getDevMinor());
		} else if (entry.isFIFO()) {
			tb.fifo();
		} else {
			throw new IOException(String.format("Unknown file type for '%s'",
					entry.getName()));
		}

		if (entry.isLink()) {
			String target = entry.getLinkName().replaceAll("/+", "/")
					.replaceAll("^/", "").replaceAll("/$", "")
					.replaceAll("^", "/");
			tb.hardlink(target);
		}

		if (entry.isFile() && !entry.isLink()) {
			tb.content(tis, entry.getSize());
		}

		tb.build();
	}

	public static void usage()
	{
		System.err.printf("Usage: %s <tar-gz-file> <squashfs-file>%n",
				SquashConvertTarGz.class.getSimpleName());
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 2) {
			usage();
		}
		convertToSquashFs(Paths.get(args[0]), Paths.get(args[1]),
				new ZlibCompression(), 0);
	}

}
