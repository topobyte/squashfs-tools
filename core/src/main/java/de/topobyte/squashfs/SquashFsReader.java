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

package de.topobyte.squashfs;

import java.io.Closeable;
import java.io.DataOutput;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import de.topobyte.squashfs.data.DataBlockCache;
import de.topobyte.squashfs.directory.DirectoryEntry;
import de.topobyte.squashfs.inode.DirectoryINode;
import de.topobyte.squashfs.inode.INode;
import de.topobyte.squashfs.inode.INodeRef;
import de.topobyte.squashfs.io.MappedFile;
import de.topobyte.squashfs.metadata.MetadataBlockCache;
import de.topobyte.squashfs.metadata.MetadataBlockReader;
import de.topobyte.squashfs.superblock.SuperBlock;
import de.topobyte.squashfs.table.ExportTable;
import de.topobyte.squashfs.table.FragmentTable;
import de.topobyte.squashfs.table.IdTable;

public interface SquashFsReader extends Closeable
{

	public static SquashFsReader fromFile(int tag, File inputFile, int offset)
			throws SquashFsException, IOException
	{
		return new FileSquashFsReader(tag, inputFile, offset);
	}

	public static SquashFsReader fromFile(int tag, File inputFile, int offset,
			MetadataBlockCache metadataCache, DataBlockCache dataCache,
			DataBlockCache fragmentCache) throws SquashFsException, IOException
	{

		return new FileSquashFsReader(tag, inputFile, offset, metadataCache,
				dataCache, fragmentCache);
	}

	public static SquashFsReader fromMappedFile(int tag, MappedFile mmap)
			throws SquashFsException, IOException
	{
		return new MappedSquashFsReader(tag, mmap);
	}

	public static SquashFsReader fromMappedFile(int tag, MappedFile mmap,
			MetadataBlockCache metadataCache, DataBlockCache dataCache,
			DataBlockCache fragmentCache) throws SquashFsException, IOException
	{
		return new MappedSquashFsReader(tag, mmap, metadataCache, dataCache,
				fragmentCache);
	}

	public SuperBlock getSuperBlock();

	public IdTable getIdTable();

	public FragmentTable getFragmentTable();

	public ExportTable getExportTable();

	public MetadataBlockReader getMetaReader();

	public DirectoryINode getRootInode() throws IOException, SquashFsException;

	public INode findInodeByInodeRef(INodeRef ref)
			throws IOException, SquashFsException;

	public INode findInodeByDirectoryEntry(DirectoryEntry entry)
			throws IOException, SquashFsException;

	public INode findInodeByPath(String path)
			throws IOException, SquashFsException, FileNotFoundException;

	public List<DirectoryEntry> getChildren(INode parent)
			throws IOException, SquashFsException;

	public long writeFileStream(INode inode, OutputStream out)
			throws IOException, SquashFsException;

	public long writeFileOut(INode inode, DataOutput out)
			throws IOException, SquashFsException;

	public int read(INode inode, long fileOffset, byte[] buf, int off, int len)
			throws IOException, SquashFsException;

}
