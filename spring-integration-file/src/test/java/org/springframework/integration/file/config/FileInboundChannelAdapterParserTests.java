/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
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

	@BeforeEach
	public void init() {
		this.accessor = new DirectFieldAccessor(inputDirPollerSource);
	}

	@Test
	public void channelName() {
		AbstractMessageChannel channel = context.getBean("inputDirPoller", AbstractMessageChannel.class);
		assertThat(channel.getComponentName()).as("Channel should be available under specified id")
				.isEqualTo("inputDirPoller");
	}

	@Test
	public void justFilter() {
		Iterator<?> filterIterator = TestUtils
				.getPropertyValue(this.inboundWithJustFilterSource, "scanner.filter.fileFilters", Set.class).iterator();
		assertThat(filterIterator.next()).isInstanceOf(IgnoreHiddenFileListFilter.class);
		assertThat(filterIterator.next()).isSameAs(this.filter);
	}

	@Test
	public void inputDirectory() {
		File expected = new File(System.getProperty("java.io.tmpdir"));
		File actual = (File) this.accessor.getPropertyValue("directory");
		assertThat(actual).as("'directory' should be set").isEqualTo(expected);
		assertThat(this.accessor.getPropertyValue("scanEachPoll")).isEqualTo(Boolean.TRUE);
		assertThat(this.inputDirPollerSource.getComponentName()).isEqualTo("inputDirPoller.adapter.source");
	}

	@Test
	public void filter() {
		DefaultDirectoryScanner scanner = (DefaultDirectoryScanner) accessor.getPropertyValue("scanner");
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(scanner);
		Object filter = scannerAccessor.getPropertyValue("filter");
		assertThat(filter instanceof AcceptOnceFileListFilter)
				.as("'filter' should be set and be of instance AcceptOnceFileListFilter but got "
						+ filter.getClass().getSimpleName()).isTrue();

		assertThat(scanner.getClass().getName()).contains("FileReadingMessageSource$WatchServiceDirectoryScanner");

		FileReadingMessageSource.WatchEventType[] watchEvents =
				(FileReadingMessageSource.WatchEventType[]) this.accessor.getPropertyValue("watchEvents");
		assertThat(watchEvents.length).isEqualTo(2);
		for (FileReadingMessageSource.WatchEventType watchEvent : watchEvents) {
			assertThat(watchEvent).isNotEqualTo(FileReadingMessageSource.WatchEventType.CREATE);
			assertThat(watchEvent)
					.isIn(FileReadingMessageSource.WatchEventType.MODIFY,
							FileReadingMessageSource.WatchEventType.DELETE);
		}
	}

	@Test
	public void comparator() {
		Object priorityQueue = accessor.getPropertyValue("toBeReceived");
		assertThat(priorityQueue).isInstanceOf(PriorityBlockingQueue.class);
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
		assertThat(actual).as("comparator reference not set, ").isSameAs(expected);
	}

	static class TestComparator implements Comparator<File> {

		@Override
		public int compare(File f1, File f2) {
			return 0;
		}

	}

}
