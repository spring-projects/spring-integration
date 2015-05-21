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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
public class FileListFilterFactoryBeanTests {

	@Test(expected = IllegalArgumentException.class)
	public void customFilterAndFilenamePatternAreMutuallyExclusive() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilter(new TestFilter());
		factory.setFilenamePattern("foo");
		factory.getObject();
	}

	@Test
	public void customFilterAndPreventDuplicatesNull() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		FileListFilter<File> result = factory.getObject();
		assertFalse(result instanceof CompositeFileListFilter);
		assertSame(testFilter, result);
	}

	@Test
	public void customFilterAndPreventDuplicatesTrue() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		factory.setPreventDuplicates(Boolean.TRUE);
		FileListFilter<File> result = factory.getObject();
		assertTrue(result instanceof CompositeFileListFilter);
		Collection<?> filters = (Collection<?>) new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		assertTrue(filters.iterator().next() instanceof AcceptOnceFileListFilter);
		assertTrue(filters.contains(testFilter));
	}

	@Test
	public void customFilterAndPreventDuplicatesFalse() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		factory.setPreventDuplicates(Boolean.FALSE);
		FileListFilter<File> result = factory.getObject();
		assertFalse(result instanceof CompositeFileListFilter);
		assertSame(testFilter, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filenamePatternAndPreventDuplicatesNull() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern("foo");
		FileListFilter<File> result = factory.getObject();
		assertTrue(result instanceof CompositeFileListFilter);
		Collection<FileListFilter<?>> filters = (Collection<FileListFilter<?>>)
				new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		Iterator<FileListFilter<?>> iterator = filters.iterator();
		assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
		assertThat(iterator.next(), is(instanceOf(SimplePatternFileListFilter.class)));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filenamePatternAndPreventDuplicatesTrue() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern(("foo"));
		factory.setPreventDuplicates(Boolean.TRUE);
		FileListFilter<File> result = factory.getObject();
		assertTrue(result instanceof CompositeFileListFilter);
		Collection<FileListFilter<?>> filters = (Collection<FileListFilter<?>>)
				new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		Iterator<FileListFilter<?>> iterator = filters.iterator();
		assertTrue(iterator.next() instanceof AcceptOnceFileListFilter);
		assertThat(iterator.next(), is(instanceOf(SimplePatternFileListFilter.class)));
	}

	@Test
	public void filenamePatternAndPreventDuplicatesFalse() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern(("foo"));
		factory.setPreventDuplicates(Boolean.FALSE);
		FileListFilter<File> result = factory.getObject();
		assertFalse(result instanceof CompositeFileListFilter);
		assertThat(result, is(instanceOf(SimplePatternFileListFilter.class)));
	}

	private static class TestFilter extends AbstractFileListFilter<File> {
		@Override
		public boolean accept(File file) {
			return true;
		}
	}

}
