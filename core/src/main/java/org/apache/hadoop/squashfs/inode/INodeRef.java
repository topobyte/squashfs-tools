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

package org.apache.hadoop.squashfs.inode;

public class INodeRef
{

	private final int location;
	private final short offset;

	public INodeRef(long value)
	{
		this.location = (int) ((value >> 16) & 0xffffffffL);
		this.offset = (short) (value & 0xffff);
	}

	public INodeRef(int location, short offset)
	{
		this.location = location;
		this.offset = offset;
	}

	public int getLocation()
	{
		return location;
	}

	public short getOffset()
	{
		return offset;
	}

	public long getRaw()
	{
		return ((((long) location) & 0xffffffffL) << 16)
				| (((long) (offset)) & 0xffffL);
	}

	@Override
	public String toString()
	{
		return String.format("{ location=%d, offset=%d }", location, offset);
	}

}
