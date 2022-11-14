/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 *
 * @see org.springframework.integration.file.config.FileInboundChannelAdapterWithPatternParserTests
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FileInboundChannelAdapterWithRegexPatternParserTests {

	private DirectFieldAccessor accessor;

	@Autowired(required = true)
	public void setSource(FileReadingMessageSource source) {
		this.accessor = new DirectFieldAccessor(source);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void regexFilter() {
		DirectFieldAccessor scannerAccessor = new DirectFieldAccessor(accessor.getPropertyValue("scanner"));
		Object extractedFilter = scannerAccessor.getPropertyValue("filter");
		assertThat(extractedFilter).isInstanceOf(CompositeFileListFilter.class);
		Set<FileListFilter<?>> filters = (Set<FileListFilter<?>>) new DirectFieldAccessor(
				extractedFilter).getPropertyValue("fileFilters");
		Pattern pattern = null;
		for (FileListFilter<?> filter : filters) {
			if (filter instanceof RegexPatternFileListFilter) {
				pattern = (Pattern) new DirectFieldAccessor(filter).getPropertyValue("pattern");
			}
		}
		assertThat(pattern).as("expected PatternMatchingFileListFilter").isNotNull();
		assertThat(pattern.pattern()).isEqualTo("^.*\\.txt$");
	}

}
