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

import static java.lang.System.lineSeparator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.topobyte.squashfs.compression.Compression;
import de.topobyte.squashfs.compression.ZlibCompression;
import de.topobyte.squashfs.data.DataBlockWriter;
import de.topobyte.squashfs.data.FragmentWriter;
import de.topobyte.squashfs.metadata.MetadataBlockRef;
import de.topobyte.squashfs.metadata.MetadataWriter;
import de.topobyte.squashfs.ra.IRandomAccess;
import de.topobyte.squashfs.ra.OffsetRandomAccess;
import de.topobyte.squashfs.ra.SimpleRandomAccess;
import de.topobyte.squashfs.superblock.SuperBlock;
import de.topobyte.squashfs.table.IdTableGenerator;

public class SquashFsWriter implements Closeable
{

	private static final Logger logger = LoggerFactory
			.getLogger(SquashFsWriter.class);

	private final Compression compression;
	private final IRandomAccess raf;
	private final IdTableGenerator idGenerator;
	private final SuperBlock superBlock;
	private final SquashFsTree fsTree;
	private final DataBlockWriter dataWriter;
	private final FragmentWriter fragmentWriter;
	private final byte[] blockBuffer;

	private Integer modificationTime = null;

	public SquashFsWriter(File outputFile) throws SquashFsException, IOException
	{
		this(outputFile, new ZlibCompression(), 0);
	}

	public SquashFsWriter(File outputFile, Compression compression, int offset)
			throws SquashFsException, IOException
	{
		this.compression = compression;
		if (offset == 0) {
			raf = new SimpleRandomAccess(outputFile, "rw");
		} else {
			raf = new OffsetRandomAccess(outputFile, "rw", offset);
		}
		writeDummySuperblock(raf);
		superBlock = createSuperBlock(compression);
		blockBuffer = createBlockBuffer(superBlock);
		idGenerator = createIdTableGenerator();
		fsTree = createSquashFsTree();
		dataWriter = createDataWriter(superBlock, raf, compression);
		fragmentWriter = createFragmentWriter(superBlock, raf);
	}

	public void setModificationTime(int modificationTime)
	{
		this.modificationTime = modificationTime;
	}

	void writeDummySuperblock(IRandomAccess raf) throws IOException
	{
		raf.seek(0);
		raf.write(new byte[SuperBlock.SIZE]);
	}

	static SuperBlock createSuperBlock(Compression compression)
	{
		return new SuperBlock(compression);
	}

	static byte[] createBlockBuffer(SuperBlock sb)
	{
		return new byte[sb.getBlockSize()];
	}

	static IdTableGenerator createIdTableGenerator()
	{
		IdTableGenerator idGenerator = new IdTableGenerator();
		idGenerator.addUidGid(0);
		return idGenerator;
	}

	SquashFsTree createSquashFsTree()
	{
		return new SquashFsTree(compression);
	}

	static DataBlockWriter createDataWriter(SuperBlock sb, IRandomAccess raf,
			Compression compression)
	{
		return new DataBlockWriter(raf, sb.getBlockSize(), compression);
	}

	FragmentWriter createFragmentWriter(SuperBlock sb, IRandomAccess raf)
	{
		return new FragmentWriter(raf, sb.getBlockSize(), compression);
	}

	SuperBlock getSuperBlock()
	{
		return superBlock;
	}

	IdTableGenerator getIdGenerator()
	{
		return idGenerator;
	}

	DataBlockWriter getDataWriter()
	{
		return dataWriter;
	}

	FragmentWriter getFragmentWriter()
	{
		return fragmentWriter;
	}

	byte[] getBlockBuffer()
	{
		return blockBuffer;
	}

	public SquashFsTree getFsTree()
	{
		return fsTree;
	}

	public SquashFsEntryBuilder entry(String name)
	{
		return new SquashFsEntryBuilder(this, name);
	}

	public void finish() throws SquashFsException, IOException
	{
		// flush any remaining fragments
		fragmentWriter.flush();

		// build the directory tree
		fsTree.build();

		// save inode table
		long inodeTableStart = raf.getFilePointer();
		logger.debug("Inode table start: {}", inodeTableStart);
		fsTree.getINodeWriter().save(raf);

		// save directory table
		long dirTableStart = raf.getFilePointer();
		logger.debug("Directory table start: {}", dirTableStart);
		fsTree.getDirWriter().save(raf);

		// build fragment table
		long fragMetaStart = raf.getFilePointer();
		MetadataWriter fragMetaWriter = new MetadataWriter(compression);
		List<MetadataBlockRef> fragRefs = fragmentWriter.save(fragMetaWriter);
		fragMetaWriter.save(raf);

		// save fragment table
		long fragTableStart = raf.getFilePointer();
		logger.debug("Fragment table start: {}", fragTableStart);
		for (MetadataBlockRef fragRef : fragRefs) {
			long fragTableFileOffset = fragMetaStart + fragRef.getLocation();
			byte[] buf = new byte[8];
			ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
			bb.putLong(fragTableFileOffset);
			raf.write(buf);
		}

		// build export table
		long exportMetaStart = raf.getFilePointer();
		MetadataWriter exportMetaWriter = new MetadataWriter(compression);
		List<MetadataBlockRef> exportRefs = fsTree
				.saveExportTable(exportMetaWriter);
		exportMetaWriter.save(raf);

		// write export table
		long exportTableStart = raf.getFilePointer();
		logger.debug("Export table start: {}", exportTableStart);
		for (MetadataBlockRef exportRef : exportRefs) {
			long exportFileOffset = exportMetaStart + exportRef.getLocation();
			byte[] buf = new byte[8];
			ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
			bb.putLong(exportFileOffset);
			raf.write(buf);
		}

		// build ID table
		long idMetaStart = raf.getFilePointer();
		MetadataWriter idMetaWriter = new MetadataWriter(compression);
		List<MetadataBlockRef> idRefs = idGenerator.save(idMetaWriter);
		idMetaWriter.save(raf);

		MetadataBlockRef rootInodeRef = fsTree.getRootInodeRef();
		logger.debug("Root inode ref: {}", rootInodeRef);

		// write ID table
		long idTableStart = raf.getFilePointer();
		logger.debug("ID table start: {}", idTableStart);

		for (MetadataBlockRef idRef : idRefs) {
			long idFileOffset = idMetaStart + idRef.getLocation();
			byte[] buf = new byte[8];
			ByteBuffer bb = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
			bb.putLong(idFileOffset);
			raf.write(buf);
		}

		long archiveSize = raf.getFilePointer();
		logger.debug("Archive size:{}", archiveSize);

		// pad to 4096 bytes
		int padding = (4096 - ((int) (archiveSize % 4096L))) % 4096;
		for (int i = 0; i < padding; i++) {
			raf.write(0);
		}

		long fileSize = raf.getFilePointer();
		logger.debug("File size: {}", fileSize);

		if (modificationTime == null) {
			modificationTime = (int) (System.currentTimeMillis() / 1000L);
		}

		// update superblock
		superBlock.setInodeCount(fsTree.getInodeCount());
		superBlock.setModificationTime(modificationTime);
		superBlock
				.setFragmentEntryCount(fragmentWriter.getFragmentEntryCount());
		superBlock.setIdCount((short) idGenerator.getIdCount());
		superBlock.setRootInodeRef(rootInodeRef.toINodeRefRaw());
		superBlock.setBytesUsed(archiveSize);
		superBlock.setIdTableStart(idTableStart);
		superBlock.setInodeTableStart(inodeTableStart);
		superBlock.setDirectoryTableStart(dirTableStart);
		superBlock.setFragmentTableStart(fragTableStart);
		superBlock.setExportTableStart(exportTableStart);

		logger.debug(lineSeparator() + "Superblock: {}", superBlock);

		// write superblock
		raf.seek(0);
		superBlock.writeData(raf);
		raf.seek(fileSize);
	}

	@Override
	public void close() throws IOException
	{
		raf.close();
	}

}
