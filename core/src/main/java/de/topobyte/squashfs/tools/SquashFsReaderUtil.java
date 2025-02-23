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
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.MappedSquashFsReader;
import de.topobyte.squashfs.SquashFsReader;
import de.topobyte.squashfs.io.MappedFile;

public class SquashFsReaderUtil
{

	final static Logger logger = LoggerFactory
			.getLogger(SquashFsReaderUtil.class);

	public static SquashFsReader createReader(Path file, boolean mapped)
			throws IOException
	{
		if (mapped) {
			logger.info("Using memory-mapped reader");
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
			logger.info("Using file reader");
			return SquashFsReader.fromFile(0, file.toFile(), 0);
		}
	}

}
