/*
 * Copyright 2002-present the original author or authors.
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

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Glenn Renfro
 */
public class FileListFilterFactoryBeanTests {

	@Test
	public void customFilterAndFilenamePatternAreMutuallyExclusive() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilter(new TestFilter());
		factory.setFilenamePattern("foo");
		assertThatIllegalArgumentException().isThrownBy(factory::getObject);
	}

	@Test
	public void customFilterAndPreventDuplicatesNull() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isFalse();
		assertThat(result).isSameAs(testFilter);
	}

	@Test
	public void customFilterAndPreventDuplicatesTrue() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		factory.setPreventDuplicates(Boolean.TRUE);
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isTrue();
		Collection<?> filters = (Collection<?>) new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		assertThat(filters.iterator().next() instanceof AcceptOnceFileListFilter).isTrue();
		assertThat(filters.contains(testFilter)).isTrue();
	}

	@Test
	public void customFilterAndPreventDuplicatesFalse() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		TestFilter testFilter = new TestFilter();
		factory.setFilter(testFilter);
		factory.setPreventDuplicates(Boolean.FALSE);
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isFalse();
		assertThat(result).isSameAs(testFilter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filenamePatternAndPreventDuplicatesNull() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern("foo");
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isTrue();
		Collection<FileListFilter<?>> filters = (Collection<FileListFilter<?>>)
				new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		Iterator<FileListFilter<?>> iterator = filters.iterator();
		assertThat(iterator.next() instanceof AcceptOnceFileListFilter).isTrue();
		assertThat(iterator.next()).isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void filenamePatternAndPreventDuplicatesTrue() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern(("foo"));
		factory.setPreventDuplicates(Boolean.TRUE);
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isTrue();
		Collection<FileListFilter<?>> filters = (Collection<FileListFilter<?>>)
				new DirectFieldAccessor(result).getPropertyValue("fileFilters");
		Iterator<FileListFilter<?>> iterator = filters.iterator();
		assertThat(iterator.next() instanceof AcceptOnceFileListFilter).isTrue();
		FileListFilter<?> patternFilter = iterator.next();
		assertThat(patternFilter).isInstanceOf(SimplePatternFileListFilter.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(patternFilter, "alwaysAcceptDirectories")).isFalse();
	}

	@Test
	public void filenamePatternAndPreventDuplicatesFalse() throws Exception {
		FileListFilterFactoryBean factory = new FileListFilterFactoryBean();
		factory.setIgnoreHidden(false);
		factory.setFilenamePattern(("foo"));
		factory.setAlwaysAcceptDirectories(true);
		factory.setPreventDuplicates(Boolean.FALSE);
		FileListFilter<File> result = factory.getObject();
		assertThat(result instanceof CompositeFileListFilter).isFalse();
		assertThat(result).isInstanceOf(SimplePatternFileListFilter.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(result, "alwaysAcceptDirectories")).isTrue();
	}

	private static class TestFilter extends AbstractFileListFilter<File> {

		@Override
		public boolean accept(File file) {
			return true;
		}

	}

}
