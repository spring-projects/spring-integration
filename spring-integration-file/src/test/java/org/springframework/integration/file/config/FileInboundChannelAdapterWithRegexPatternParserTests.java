/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
