/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.file.TestFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@SpringJUnitConfig
@DirtiesContext
public class FileInboundChannelAdapterWithPreventDuplicatesFlagTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("testFilter")
	private TestFileListFilter testFilter;

	@Test
	public void filterAndNull() {
		FileListFilter<?> filter = this.extractFilter("filterAndNull");
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter).isSameAs(testFilter);
	}

	@Test
	public void filterAndTrue() {
		FileListFilter<?> filter = this.extractFilter("filterAndTrue");
		assertThat(filter instanceof CompositeFileListFilter).isTrue();
		Collection<?> filters = (Collection<?>) new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		assertThat(filters.iterator().next() instanceof AcceptOnceFileListFilter).isTrue();
		assertThat(filters.contains(testFilter)).isTrue();
	}

	@Test
	public void filterAndFalse() {
		FileListFilter<?> filter = this.extractFilter("filterAndFalse");
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter).isSameAs(testFilter);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternAndNull() {
		FileListFilter<?> filter = this.extractFilter("patternAndNull");
		assertThat(filter instanceof CompositeFileListFilter).isTrue();
		Collection<FileListFilter<File>> filters = (Collection<FileListFilter<File>>)
				new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		Iterator<FileListFilter<File>> iterator = filters.iterator();
		assertThat(iterator.next() instanceof AcceptOnceFileListFilter).isTrue();
		assertThat(iterator.next()).isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternAndTrue() {
		FileListFilter<?> filter = this.extractFilter("patternAndTrue");
		assertThat(filter instanceof CompositeFileListFilter).isTrue();
		Collection<FileListFilter<File>> filters = (Collection<FileListFilter<File>>)
				new DirectFieldAccessor(filter).getPropertyValue("fileFilters");
		Iterator<FileListFilter<File>> iterator = filters.iterator();
		assertThat(iterator.next() instanceof AcceptOnceFileListFilter).isTrue();
		assertThat(iterator.next()).isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	public void patternAndFalse() {
		FileListFilter<File> filter = this.extractFilter("patternAndFalse");
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter).isInstanceOf(SimplePatternFileListFilter.class);
	}

	@Test
	public void defaultAndNull() {
		FileListFilter<File> filter = this.extractFilter("defaultAndNull");
		assertThat(filter).isNotNull();
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter instanceof AcceptOnceFileListFilter).isTrue();

		File testFile = new File("test");
		File[] files = new File[] {testFile, testFile, testFile};
		List<File> result = filter.filterFiles(files);
		assertThat(result.size()).isEqualTo(1);
	}

	@Test
	public void defaultAndTrue() {
		FileListFilter<File> filter = this.extractFilter("defaultAndTrue");
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter instanceof AcceptOnceFileListFilter).isTrue();
		File testFile = new File("test");
		File[] files = new File[] {testFile, testFile, testFile};
		List<File> result = filter.filterFiles(files);
		assertThat(result.size()).isEqualTo(1);
	}

	@Test
	public void defaultAndFalse() {
		FileListFilter<File> filter = this.extractFilter("defaultAndFalse");
		assertThat(filter).isNotNull();
		assertThat(filter instanceof CompositeFileListFilter).isFalse();
		assertThat(filter instanceof AcceptOnceFileListFilter).isFalse();
		File testFile = new File("test");
		File[] files = new File[] {testFile, testFile, testFile};
		List<File> result = filter.filterFiles(files);
		assertThat(result.size()).isEqualTo(3);
	}

	@SuppressWarnings("unchecked")
	private FileListFilter<File> extractFilter(String beanName) {
		return (FileListFilter<File>) TestUtils.getPropertyValue(context.getBean(beanName), "source.scanner.filter");
	}

}
