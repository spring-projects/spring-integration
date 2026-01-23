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
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FileInboundChannelAdapterWithPatternParserTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	@Qualifier("adapterWithPattern.adapter")
	private AbstractEndpoint endpoint;

	@Autowired
	FileReadingMessageSource source;

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
		File actual = TestUtils.getPropertyValue(this.source, "directoryExpression.value");
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void compositeFilterType() {
		FileListFilter<?> filter = TestUtils.getPropertyValue(this.source, "scanner.filter");
		assertThat(filter).isInstanceOf(CompositeFileListFilter.class);
	}

	@Test
	public void compositeFilterSetSize() {
		assertThat((Set<?>) TestUtils.getPropertyValue(this.source, "scanner.filter.fileFilters")).hasSize(2);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void acceptOnceFilter() {
		var filters = (Set<FileListFilter<File>>) TestUtils.getPropertyValue(this.source, "scanner.filter.fileFilters");
		boolean hasAcceptOnceFilter = false;
		for (FileListFilter<File> filter : filters) {
			if (filter instanceof AcceptOnceFileListFilter) {
				hasAcceptOnceFilter = true;
				break;
			}
		}
		assertThat(hasAcceptOnceFilter).as("expected AcceptOnceFileListFilter").isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void patternFilter() {
		var filters = (Set<FileListFilter<File>>) TestUtils.getPropertyValue(this.source, "scanner.filter.fileFilters");
		String pattern = null;
		for (FileListFilter<?> filter : filters) {
			if (filter instanceof SimplePatternFileListFilter) {
				pattern = (String) new DirectFieldAccessor(filter).getPropertyValue("path");
			}
		}
		assertThat(pattern).as("expected SimplePatternFileListFilterTest").isNotNull();
		assertThat(pattern).isEqualTo("*.txt");
	}

}
