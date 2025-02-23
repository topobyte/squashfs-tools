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

package de.topobyte.squashfs.metadata;

import java.io.File;
import java.io.IOException;

import de.topobyte.squashfs.SquashFsException;
import de.topobyte.squashfs.ra.IRandomAccess;
import de.topobyte.squashfs.ra.SimpleRandomAccess;
import de.topobyte.squashfs.superblock.SuperBlock;

public class FileMetadataBlockReader implements MetadataBlockReader
{

	private final int tag;
	private final IRandomAccess raf;
	private final SuperBlock sb;
	private final boolean shouldClose;

	public FileMetadataBlockReader(int tag, File file)
			throws IOException, SquashFsException
	{
		this.tag = tag;
		this.raf = new SimpleRandomAccess(file, "r");
		this.sb = SuperBlock.read(raf);
		this.shouldClose = true;
	}

	public FileMetadataBlockReader(int tag, IRandomAccess raf, SuperBlock sb,
			boolean shouldClose)
	{
		this.tag = tag;
		this.raf = raf;
		this.sb = sb;
		this.shouldClose = shouldClose;
	}

	@Override
	public SuperBlock getSuperBlock(int tag)
	{
		if (this.tag != tag) {
			throw new IllegalArgumentException(
					String.format("Invalid tag: %d", tag));
		}
		return sb;
	}

	@Override
	public MetadataBlock read(int tag, long fileOffset)
			throws IOException, SquashFsException
	{
		if (this.tag != tag) {
			throw new IllegalArgumentException(
					String.format("Invalid tag: %d", tag));
		}
		long prevOffset = raf.getFilePointer();
		try {
			raf.seek(fileOffset);
			MetadataBlock block = MetadataBlock.read(raf, sb);
			return block;
		} finally {
			raf.seek(prevOffset);
		}
	}

	@Override
	public void close() throws IOException
	{
		if (shouldClose) {
			raf.close();
		}
	}

}
