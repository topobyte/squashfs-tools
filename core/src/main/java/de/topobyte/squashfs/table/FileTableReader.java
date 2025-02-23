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

package de.topobyte.squashfs.table;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.topobyte.squashfs.SquashFsException;
import de.topobyte.squashfs.ra.IRandomAccess;
import de.topobyte.squashfs.ra.SimpleRandomAccess;
import de.topobyte.squashfs.superblock.SuperBlock;

public class FileTableReader implements TableReader
{

	private final IRandomAccess raf;
	private final SuperBlock sb;
	private final boolean shouldClose;

	public FileTableReader(File file) throws IOException, SquashFsException
	{
		this.raf = new SimpleRandomAccess(file, "r");
		this.sb = SuperBlock.read(raf);
		this.shouldClose = true;
	}

	public FileTableReader(IRandomAccess raf, SuperBlock sb,
			boolean shouldClose)
	{
		this.raf = raf;
		this.sb = sb;
		this.shouldClose = shouldClose;
	}

	@Override
	public SuperBlock getSuperBlock()
	{
		return sb;
	}

	@Override
	public ByteBuffer read(long fileOffset, int length) throws IOException
	{
		long prevPosition = raf.getFilePointer();
		try {
			raf.seek(fileOffset);
			byte[] buf = new byte[length];
			raf.readFully(buf);
			return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
		} finally {
			raf.seek(prevPosition);
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
