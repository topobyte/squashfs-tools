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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.SquashFsReader;

public class RunSquashExtract
{

	final static Logger logger = LoggerFactory
			.getLogger(RunSquashExtract.class);

	public static void usage()
	{
		System.err.printf(
				"Usage: %s [options...] <squashfs-file> <directory>%n",
				RunSquashExtract.class.getSimpleName());
		System.err.println();
		System.err.println("    -m,--mapped     Use mmap() for I/O");
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception
	{
		boolean mapped = false;

		String squashfs = null;
		String dir = null;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
			case "-m":
			case "--mapped":
				mapped = true;
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
			logger.warn("Output directory '{}' exists. Exit.", dir);
			System.exit(1);
		}

		SquashExtract task = new SquashExtract();
		try (SquashFsReader reader = SquashFsReaderUtil
				.createReader(Paths.get(squashfs), 0, mapped)) {
			logger.info(lineSeparator() + reader.getSuperBlock());
			logger.info(lineSeparator() + reader.getIdTable());
			logger.info(lineSeparator() + reader.getFragmentTable());
			logger.info(lineSeparator() + reader.getExportTable());

			task.extract(reader, directory);
		}
	}

}
