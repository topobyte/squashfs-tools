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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import de.topobyte.squashfs.SquashFsReader;

public class RunSquashExtract
{

	public static void usage()
	{
		System.err.printf(
				"Usage: %s [options...] <squashfs-file> <directory>%n",
				RunSquashExtract.class.getSimpleName());
		System.err.println();
		System.err.println("    -m,--mapped     Use mmap() for I/O");
		System.err.println("       --metadata   Dump metadata ");
		System.err
				.println("                       <file-offset> <block-offset>");
		System.err.println();
		System.exit(1);
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

		SquashExtract task = new SquashExtract();
		try (SquashFsReader reader = SquashFsReaderUtil
				.createReader(Paths.get(squashfs), mapped)) {
			System.out.println(reader.getSuperBlock());
			System.out.println();
			System.out.println(reader.getIdTable());
			System.out.println();
			System.out.println(reader.getFragmentTable());
			System.out.println();
			System.out.println(reader.getExportTable());
			System.out.println();

			task.extract(reader, directory);

			if (metadata) {
				task.dumpMetadataBlock(reader, metaFileOffset, metaBlockOffset);
			}
		}
	}

}
