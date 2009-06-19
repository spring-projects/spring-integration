/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.file.CompositeFileListFilter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterParserTests {

	@Autowired(required=true)
	private ApplicationContext context;

	@Autowired(required=true)
	private FileReadingMessageSource source;

	private DirectFieldAccessor accessor;


	@Before
	public void init() {
		accessor = new DirectFieldAccessor(source);
	}


	@Test
	public void channelName() throws Exception {
		MessageChannel channel = (MessageChannel) context.getBean("inputDirPoller");
		assertEquals("Channel should be available under specified id", "inputDirPoller", channel.getName());		
	}

	@Test
	public void inputDirectory() {	
		File expected = new File(System.getProperty("java.io.tmpdir"));
		File actual = (File) accessor.getPropertyValue("inputDirectory");
		assertEquals("'inputDirectory' should be set", expected, actual);
	}

	@Test
	public void filter() throws Exception {
		assertTrue("'filter' should be set", accessor.getPropertyValue("filter") instanceof CompositeFileListFilter);
	}

	@Test
	public void comparator() throws Exception {
		Object priorityQueue = accessor.getPropertyValue("toBeReceived");
		assertEquals(PriorityBlockingQueue.class, priorityQueue.getClass());
		Object expected = context.getBean("testComparator");
		Object innerQueue = new DirectFieldAccessor(priorityQueue).getPropertyValue("q");
		Object actual = new DirectFieldAccessor(innerQueue).getPropertyValue("comparator");
		assertSame("comparator reference not set, ", expected, actual);
	}


	static class TestComparator implements Comparator<File> {

		public int compare(File f1, File f2) {
			return 0;
		}
	}

}
