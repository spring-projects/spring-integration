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
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithPatternParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("adapterWithPattern.adapter")
	private AbstractEndpoint endpoint;

	private DirectFieldAccessor accessor;

	@Autowired
	public void setSource(FileReadingMessageSource source) {
		this.accessor = new DirectFieldAccessor(source);
	}

	@Test
	public void channelName() {
		AbstractMessageChannel channel = context.getBean("adapterWithPattern", AbstractMessageChannel.class);
		assertThat(channel.getComponentName()).isEqualTo("adapterWithPattern");
	}

	@Test
	public void autoStartupDisabled() {
		assertThat(this.endpoint.isRunning()).isFalse();
		assertThat(new DirectFieldAccessor(endpoint).getPropertyValue("autoStartup")).isEqualTo(Boolean.FALSE);
	}

	@Test
	public void inputDirectory() {
		File expected = new File(System.getProperty("java.io.tmpdir"));
		File actual = (File) accessor.getPropertyValue("directory");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void compositeFilterType() {
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
		assertThat(scannerAccessor.getPropertyValue("filter") instanceof CompositeFileListFilter).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void compositeFilterSetSize() {
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
		Set<FileListFilter<File>> filters = (Set<FileListFilter<File>>) new DirectFieldAccessor(
				scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		assertThat(filters.size()).isEqualTo(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void acceptOnceFilter() {
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
		Set<FileListFilter<File>> filters = (Set<FileListFilter<File>>) new DirectFieldAccessor(
				scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		boolean hasAcceptOnceFilter = false;
		for (FileListFilter<File> filter : filters) {
			if (filter instanceof AcceptOnceFileListFilter) {
				hasAcceptOnceFilter = true;
			}
		}
		assertThat(hasAcceptOnceFilter).as("expected AcceptOnceFileListFilter").isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternFilter() {
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
		Set<FileListFilter<?>> filters = (Set<FileListFilter<?>>) new DirectFieldAccessor(
				scannerAccessor.getPropertyValue("filter")).getPropertyValue("fileFilters");
		String pattern = null;
		for (FileListFilter<?> filter : filters) {
			if (filter instanceof SimplePatternFileListFilter) {
				pattern = (String) new DirectFieldAccessor(filter).getPropertyValue("path");
			}
		}
		assertThat(pattern).as("expected SimplePatternFileListFilterTest").isNotNull();
		assertThat(pattern.toString()).isEqualTo("*.txt");
	}

}
