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

import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.compression.ZstdCompression;

public class RunSquashConvertDirectory
{

	final static Logger logger = LoggerFactory
			.getLogger(RunSquashConvertDirectory.class);

	public static void usage()
	{
		System.err.printf("Usage: %s <directory> <squashfs-file>%n",
				RunSquashConvertDirectory.class.getSimpleName());
		System.err.println();
		System.exit(1);
	}

	public static void main(String[] args) throws Exception
	{
		if (args.length != 2) {
			usage();
		}
		SquashConvertDirectory task = new SquashConvertDirectory();
		task.convertToSquashFs(Paths.get(args[0]), Paths.get(args[1]),
				new ZstdCompression(), 0);
	}

}
