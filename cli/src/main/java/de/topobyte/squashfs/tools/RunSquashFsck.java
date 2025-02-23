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

import static java.lang.System.lineSeparator;

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.SquashFsReader;

public class RunSquashFsck
{

	final static Logger logger = LoggerFactory.getLogger(RunSquashFsck.class);

	public static void usage()
	{
		System.err.printf("Usage: %s [options...] <squashfs-file>%n",
				RunSquashFsck.class.getSimpleName());
		System.err.println();
		System.err.println("    -m,--mapped     Use mmap() for I/O");
		System.err.println("    -t,--tree       Dump tree");
		System.err
				.println("    -f,--files      Read all files (implies --tree)");
		System.err.println("       --metadata   Dump metadata ");
		System.err
				.println("                       <file-offset> <block-offset>");
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception
	{
		boolean mapped = false;
		boolean tree = false;
		boolean files = false;
		boolean metadata = false;
		long metaFileOffset = 0L;
		int metaBlockOffset = 0;

		String squashfs = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
			case "-m":
			case "--mapped":
				mapped = true;
				break;
			case "-t":
			case "--tree":
				tree = true;
				break;
			case "-f":
			case "--files":
				files = true;
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
				if (squashfs != null) {
					usage();
				}
				squashfs = arg;
			}
		}
		if (squashfs == null) {
			usage();
		}

		SquashFsck task = new SquashFsck();
		try (SquashFsReader reader = SquashFsReaderUtil
				.createReader(Paths.get(squashfs), mapped)) {
			logger.info(lineSeparator() + reader.getSuperBlock());
			logger.info(lineSeparator() + reader.getIdTable());
			logger.info(lineSeparator() + reader.getFragmentTable());
			logger.info(lineSeparator() + reader.getExportTable());

			if (tree || files) {
				task.dumpTree(reader, files);
			}

			if (metadata) {
				task.dumpMetadataBlock(reader, metaFileOffset, metaBlockOffset);
			}
		}
	}

}
