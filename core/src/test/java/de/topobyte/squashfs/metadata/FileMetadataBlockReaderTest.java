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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.topobyte.squashfs.ra.IRandomAccess;
import de.topobyte.squashfs.ra.SimpleRandomAccess;
import de.topobyte.squashfs.superblock.SuperBlock;
import de.topobyte.squashfs.test.MetadataTestUtils;

public class FileMetadataBlockReaderTest
{

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	int tag;
	File tempFile;
	FileMetadataBlockReader reader;
	SuperBlock sb;
	byte[] block;
	byte[] encoded;

	@Before
	public void setUp() throws Exception
	{
		tag = 1;
		tempFile = temp.newFile();
		sb = new SuperBlock();
		try (IRandomAccess raf = new SimpleRandomAccess(tempFile, "rw")) {
			sb.writeData(raf);

			// write a block
			block = new byte[1024];
			for (int i = 0; i < block.length; i++) {
				block[i] = (byte) (i & 0xff);
			}
			encoded = MetadataTestUtils.saveMetadataBlock(block);
			raf.write(encoded);
		}

		reader = new FileMetadataBlockReader(tag, tempFile);
	}

	@After
	public void tearDown() throws Exception
	{
		reader.close();
		reader = null;
		encoded = null;
		block = null;
		sb = null;
	}

	@Test
	public void getSuperBlockShouldReturnVersionReadFromFile()
	{
		assertEquals(sb.getModificationTime(),
				reader.getSuperBlock(tag).getModificationTime());
	}

	@Test
	public void getSuperBlockShouldReturnConstructedVersionIfApplicable()
			throws Exception
	{
		try (IRandomAccess raf = new SimpleRandomAccess(tempFile, "r")) {
			reader = new FileMetadataBlockReader(tag, raf, sb, true);
		}
		assertSame(sb, reader.getSuperBlock(tag));
	}

	@Test
	public void readFromFileOffsetShouldSucceed() throws Exception
	{
		MetadataBlock mb = reader.read(tag, SuperBlock.SIZE);
		assertEquals(1024, mb.data.length);
		assertArrayEquals(block, mb.data);
	}

	@Test
	public void readFromFileOffsetOnRandomAccessFileBackedReaderShouldSucceed()
			throws Exception
	{
		try (IRandomAccess raf = new SimpleRandomAccess(tempFile, "r")) {
			reader = new FileMetadataBlockReader(tag, raf, sb, true);
			MetadataBlock mb = reader.read(tag, SuperBlock.SIZE);
			assertEquals(1024, mb.data.length);
			assertArrayEquals(block, mb.data);
		}
	}

	@Test
	public void closeShouldCloseUnderlyingReaderIfRequested() throws Exception
	{
		try (IRandomAccess raf = new SimpleRandomAccess(tempFile, "r")) {
			reader = new FileMetadataBlockReader(tag, raf, sb, true);
			reader.close();

			try {
				raf.seek(0L);
				fail("exception not thrown");
			} catch (IOException e) {
				System.out.println("EOF");
			}
		}
	}

	@Test
	public void closeShouldNotCloseUnderlyingReaderIfNotRequested()
			throws Exception
	{
		try (IRandomAccess raf = new SimpleRandomAccess(tempFile, "r")) {
			reader = new FileMetadataBlockReader(tag, raf, sb, false);
			reader.close();
			raf.seek(0L);
		}
	}

}
