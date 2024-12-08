package org.apache.hadoop.squashfs;

import java.io.IOException;
import java.io.OutputStream;

import com.github.luben.zstd.ZstdOutputStream;

public class CompressionUtil
{

	public static ZstdOutputStream createZstdOutputStream(OutputStream os)
			throws IOException
	{
		return new ZstdOutputStream(os, 18);
	}

}
