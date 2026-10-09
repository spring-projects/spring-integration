/*
 * Copyright 2002-present the original author or authors.
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
