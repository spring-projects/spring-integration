/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.file.DefaultDirectoryScanner;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class FileInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("inputDirPoller.adapter.source")
	private FileReadingMessageSource inputDirPollerSource;

	@Autowired
	@Qualifier("inboundWithJustFilter.adapter.source")
	private FileReadingMessageSource inboundWithJustFilterSource;

	@Autowired
	private FileListFilter<File> filter;

	private DirectFieldAccessor accessor;

	@Before
	public void init() {
		this.accessor = new DirectFieldAccessor(inputDirPollerSource);
	}

	@Test
	public void channelName() throws Exception {
		AbstractMessageChannel channel = context.getBean("inputDirPoller", AbstractMessageChannel.class);
		assertEquals("Channel should be available under specified id", "inputDirPoller", channel.getComponentName());
	}

	@Test
	public void justFilter() throws Exception {
		Iterator<?> filterIterator = TestUtils
				.getPropertyValue(this.inboundWithJustFilterSource, "scanner.filter.fileFilters", Set.class).iterator();
		assertThat(filterIterator.next(), instanceOf(IgnoreHiddenFileListFilter.class));
		assertSame(this.filter, filterIterator.next());
	}

	@Test
	public void inputDirectory() {
		File expected = new File(System.getProperty("java.io.tmpdir"));
		File actual = (File) this.accessor.getPropertyValue("directory");
		assertEquals("'directory' should be set", expected, actual);
		assertThat(this.accessor.getPropertyValue("scanEachPoll"), is(Boolean.TRUE));
	}

	@Test
	public void filter() throws Exception {
		DefaultDirectoryScanner scanner = (DefaultDirectoryScanner) accessor.getPropertyValue("scanner");
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(scanner);
		Object filter = scannerAccessor.getPropertyValue("filter");
		assertTrue("'filter' should be set and be of instance AcceptOnceFileListFilter but got "
			+ filter.getClass().getSimpleName(), filter instanceof AcceptOnceFileListFilter);

		assertThat(scanner.getClass().getName(),
				containsString("FileReadingMessageSource$WatchServiceDirectoryScanner"));

		FileReadingMessageSource.WatchEventType[] watchEvents =
				(FileReadingMessageSource.WatchEventType[]) this.accessor.getPropertyValue("watchEvents");
		assertEquals(2, watchEvents.length);
		for (FileReadingMessageSource.WatchEventType watchEvent : watchEvents) {
			assertNotEquals(FileReadingMessageSource.WatchEventType.CREATE, watchEvent);
			assertThat(watchEvent, isOneOf(FileReadingMessageSource.WatchEventType.MODIFY,
					FileReadingMessageSource.WatchEventType.DELETE));
		}
	}

	@Test
	public void comparator() throws Exception {
		Object priorityQueue = accessor.getPropertyValue("toBeReceived");
		assertEquals(PriorityBlockingQueue.class, priorityQueue.getClass());
		Object expected = context.getBean("testComparator");
		DirectFieldAccessor queueAccessor = new DirectFieldAccessor(priorityQueue);
		Object innerQueue = queueAccessor.getPropertyValue("q");
		Object actual;
		if (innerQueue != null) {
			actual = new DirectFieldAccessor(innerQueue).getPropertyValue("comparator");
		}
		else {
			// probably running under JDK 7
			actual = queueAccessor.getPropertyValue("comparator");
		}
		assertSame("comparator reference not set, ", expected, actual);
	}

	static class TestComparator implements Comparator<File> {

		@Override
		public int compare(File f1, File f2) {
			return 0;
		}
	}
}
