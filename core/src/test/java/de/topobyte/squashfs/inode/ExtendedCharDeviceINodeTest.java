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

package de.topobyte.squashfs.inode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Before;
import org.junit.Test;

public class ExtendedCharDeviceINodeTest
{

	ExtendedCharDeviceINode inode;

	@Before
	public void setUp()
	{
		inode = new ExtendedCharDeviceINode();
		inode.setDevice(1);
		inode.setNlink(2);
		inode.setXattrIndex(3);
	}

	@Test
	public void getNameShouldReturnCorrectValue()
	{
		assertEquals("extended-char-dev-inode", inode.getName());
	}

	@Test
	public void getInodeTypeShouldReturnCorrectValue()
	{
		assertSame(INodeType.EXTENDED_CHAR_DEVICE, inode.getInodeType());
	}

	@Test
	public void simplifyShouldReturnOriginalIfExtendedAttributesPresent()
	{
		CharDeviceINode inode2 = new ExtendedCharDeviceINode();
		inode2.setDevice(1);
		inode2.setNlink(2);
		inode2.setXattrIndex(3);
		assertSame(inode2, inode2.simplify());
	}

	@Test
	public void simplifyShouldReturnBasicIfExtendedAttributesNotPresent()
	{
		CharDeviceINode inode2 = new ExtendedCharDeviceINode();
		inode2.setDevice(1);
		inode2.setNlink(2);
		inode2.setXattrIndex(-1);

		CharDeviceINode result = inode2.simplify();
		assertSame("wrong class", BasicCharDeviceINode.class,
				result.getClass());
		assertSame("wrong device", 1, result.getDevice());
		assertSame("wrong nlink count", 2, result.getNlink());
	}

	@Test
	public void toStringShouldNotFail()
	{
		System.out.println(inode.toString());
	}

}
