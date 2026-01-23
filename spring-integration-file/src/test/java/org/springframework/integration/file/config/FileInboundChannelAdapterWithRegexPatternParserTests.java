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

import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @see org.springframework.integration.file.config.FileInboundChannelAdapterWithPatternParserTests
 */
@SpringJUnitConfig
@DirtiesContext
public class FileInboundChannelAdapterWithRegexPatternParserTests {

	@Autowired
	FileReadingMessageSource source;

	@Test
	@SuppressWarnings("unchecked")
	public void regexFilter() {
		var filters = (Set<FileListFilter<?>>) TestUtils.getPropertyValue(this.source, "scanner.filter.fileFilters");
		Pattern pattern = null;
		for (FileListFilter<?> filter : filters) {
			if (filter instanceof RegexPatternFileListFilter) {
				pattern = TestUtils.<Pattern>getPropertyValue(filter, "pattern");
				break;
			}
		}
		assertThat(pattern).as("expected PatternMatchingFileListFilter").isNotNull();
		assertThat(pattern.pattern()).isEqualTo("^.*\\.txt$");
	}

}
