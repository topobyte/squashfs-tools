package org.apache.hadoop.squashfs.ra;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * An implementation for IRandomAccess that passes all calls to an encapsulated
 * RandomAccessFile, but reads and writes to a virtual file within the file
 * itself that starts at a specified offset.
 */
public class OffsetRandomAccess implements IRandomAccess
{

	private RandomAccessFile raf;
	private int offset;

	public OffsetRandomAccess(File file, String mode, int offset)
			throws FileNotFoundException
	{
		this.offset = offset;
		raf = new RandomAccessFile(file, mode);
	}

	@Override
	public void seek(long offset) throws IOException
	{
		raf.seek(this.offset + offset);
	}

	@Override
	public void write(int b) throws IOException
	{
		raf.write(b);
	}

	@Override
	public void write(byte[] data) throws IOException
	{
		raf.write(data);
	}

	@Override
	public long getFilePointer() throws IOException
	{
		return raf.getFilePointer() - offset;
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException
	{
		raf.write(data, offset, length);
	}

	@Override
	public void close() throws IOException
	{
		raf.close();
	}

	// DataInput

	@Override
	public int skipBytes(int n) throws IOException
	{
		return raf.skipBytes(n);
	}

	@Override
	public boolean readBoolean() throws IOException
	{
		return raf.readBoolean();
	}

	@Override
	public byte readByte() throws IOException
	{
		return raf.readByte();
	}

	@Override
	public int readUnsignedByte() throws IOException
	{
		return raf.readUnsignedByte();
	}

	@Override
	public short readShort() throws IOException
	{
		return raf.readShort();
	}

	@Override
	public int readUnsignedShort() throws IOException
	{
		return raf.readUnsignedShort();
	}

	@Override
	public char readChar() throws IOException
	{
		return raf.readChar();
	}

	@Override
	public int readInt() throws IOException
	{
		return raf.readInt();
	}

	@Override
	public long readLong() throws IOException
	{
		return raf.readLong();
	}

	@Override
	public float readFloat() throws IOException
	{
		return raf.readFloat();
	}

	@Override
	public double readDouble() throws IOException
	{
		return raf.readDouble();
	}

	@Override
	public String readLine() throws IOException
	{
		return raf.readLine();
	}

	@Override
	public String readUTF() throws IOException
	{
		return raf.readUTF();
	}

	// DataOutput

	@Override
	public void readFully(byte[] buf) throws IOException
	{
		raf.readFully(buf);
	}

	@Override
	public void readFully(byte[] buf, int off, int len) throws IOException
	{
		raf.readFully(buf, off, len);
	}

	@Override
	public void writeBoolean(boolean v) throws IOException
	{
		raf.writeBoolean(v);
	}

	@Override
	public void writeByte(int v) throws IOException
	{
		raf.writeByte(v);
	}

	@Override
	public void writeShort(int v) throws IOException
	{
		raf.writeShort(v);
	}

	@Override
	public void writeChar(int v) throws IOException
	{
		raf.writeChar(v);
	}

	@Override
	public void writeInt(int v) throws IOException
	{
		raf.writeInt(v);
	}

	@Override
	public void writeLong(long v) throws IOException
	{
		raf.writeLong(v);
	}

	@Override
	public void writeFloat(float v) throws IOException
	{
		raf.writeFloat(v);
	}

	@Override
	public void writeDouble(double v) throws IOException
	{
		raf.writeDouble(v);
	}

	@Override
	public void writeBytes(String s) throws IOException
	{
		raf.writeBytes(s);
	}

	@Override
	public void writeChars(String s) throws IOException
	{
		raf.writeChars(s);
	}

	@Override
	public void writeUTF(String s) throws IOException
	{
		raf.writeUTF(s);
	}

}
