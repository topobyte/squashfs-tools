package org.apache.hadoop.squashfs.ra;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface to avoid using RandomAccessFile directly. We want to support a
 * special implementation that writes the image at an offset and can achieve
 * this by using this interface instead of RandomAccessFile across the source
 * code.
 */
public interface IRandomAccess extends DataInput, DataOutput, Closeable
{

	void seek(long offset) throws IOException;

	@Override
	void write(int i) throws IOException;

	@Override
	void write(byte[] data) throws IOException;

	long getFilePointer() throws IOException;

	@Override
	void write(byte[] data, int off, int len) throws IOException;

	@Override
	void close() throws IOException;

	@Override
	void readFully(byte[] buf) throws IOException;

	@Override
	void readFully(byte[] buf, int off, int len) throws IOException;

}
