/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.TestFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithPreventDuplicatesFlagTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("testFilter")
	private TestFileListFilter testFilter;


	@Test
	public void filterAndNull() {
		FileListFilter<?> filter = this.extractFilter("filterAndNull");
		assertFalse(filter instanceof CompositeFileListFilter);
		assertSame(testFilter, filter);
	}

	@Test
	public void filterAndTrue() {
		FileListFilter<?> filter = this.extractFilter("filterAndTrue");
		assertTrue(filter instanceof CompositeFileListFilter);
		Collection<?> filters = (Collection<?>) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		assertTrue(filters.iterator().next() instanceof AcceptOnceFileListFilter);
		assertTrue(filters.contains(testFilter));
	}

	@Test
	public void filterAndFalse() throws Exception {
		FileListFilter<?> filter = this.extractFilter("filterAndFalse");
		assertFalse(filter instanceof CompositeFileListFilter);
		assertSame(testFilter, filter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternAndNull() throws Exception {
		FileListFilter<?> filter = this.extractFilter("patternAndNull");
		assertTrue(filter instanceof CompositeFileListFilter);
		Collection<FileListFilter<File>> filters = (Collection<FileListFilter<File>>)
				new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		Iterator<FileListFilter<File>> iterator = filters.iterator();
		assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
		assertThat(iterator.next(), is(instanceOf(SimplePatternFileListFilter.class)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternAndTrue() throws Exception {
		FileListFilter<?> filter = this.extractFilter("patternAndTrue");
		assertTrue(filter instanceof CompositeFileListFilter);
		Collection<FileListFilter<File>> filters = (Collection<FileListFilter<File>>)
				new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		Iterator<FileListFilter<File>> iterator = filters.iterator();
		assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
		assertThat(iterator.next(), is(instanceOf(SimplePatternFileListFilter.class)));
	}

	@Test
	public void patternAndFalse() throws Exception {
		FileListFilter<File> filter = this.extractFilter("patternAndFalse");
		assertFalse(filter instanceof CompositeFileListFilter);
		assertThat(filter, is(instanceOf(SimplePatternFileListFilter.class)));
	}

	@Test
	public void defaultAndNull() throws Exception {
		FileListFilter<File> filter = this.extractFilter("defaultAndNull");
		assertNotNull(filter);
		assertFalse(filter instanceof CompositeFileListFilter);
		assertTrue(filter instanceof AcceptOnceFileListFilter);

		File testFile = new File("test");
		File[] files = new File[] { testFile, testFile, testFile };
		List<File> result = filter.filterFiles(files);
		assertEquals(1, result.size());
	}

	@Test
	public void defaultAndTrue() throws Exception {
		FileListFilter<File> filter = this.extractFilter("defaultAndTrue");
		assertFalse(filter instanceof CompositeFileListFilter);
		assertTrue(filter instanceof AcceptOnceFileListFilter);
		File testFile = new File("test");
		File[] files = new File[] { testFile, testFile, testFile };
		List<File> result = filter.filterFiles(files);
		assertEquals(1, result.size());
	}

	@Test
	public void defaultAndFalse() throws Exception {
		FileListFilter<File> filter = this.extractFilter("defaultAndFalse");
		assertNotNull(filter);
		assertFalse(filter instanceof CompositeFileListFilter);
		assertFalse(filter instanceof AcceptOnceFileListFilter);
		File testFile = new File("test");
		File[] files = new File[] { testFile, testFile, testFile };
		List<File> result = filter.filterFiles(files);
		assertEquals(3, result.size());
	}


	@SuppressWarnings("unchecked")
	private FileListFilter<File> extractFilter(String beanName) {
		return (FileListFilter<File>)
				new DirectFieldAccessor(
						new DirectFieldAccessor(
								new DirectFieldAccessor(context.getBean(beanName))
										.getPropertyValue("source"))
								.getPropertyValue("scanner"))
						.getPropertyValue("filter");
	}

}
