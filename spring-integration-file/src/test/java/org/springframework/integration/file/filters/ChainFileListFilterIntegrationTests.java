/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Aaron Grant
 * @author Cengis Kocaurlu
 *
 * @since 4.3.8
 */
public class ChainFileListFilterIntegrationTests {

	private static final String PATTERN_ANY_TEXT_FILES = "*.txt";

	private File[] noFiles = new File[0];

	private File[] oneFile = new File[] {new MockOldFile("file.txt")};

	@Test
	public void singleModifiedFilterNoFiles() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(noFiles);
			assertThat(result.size()).isEqualTo(0);
		}
	}

	@Test
	public void singlePatternFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter(PATTERN_ANY_TEXT_FILES));
			List<File> result = chain.filterFiles(oneFile);
			assertThat(result.size()).isEqualTo(1);
		}
	}

	@Test
	public void singleModifiedFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertThat(result.size()).isEqualTo(1);
		}
	}

	@Test
	public void patternThenModifiedFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter(PATTERN_ANY_TEXT_FILES));
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertThat(result.size()).isEqualTo(1);
		}
	}

	@Test
	public void modifiedThenPatternFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			chain.addFilter(new SimplePatternFileListFilter(PATTERN_ANY_TEXT_FILES));
			List<File> result = chain.filterFiles(oneFile);
			assertThat(result.size()).isEqualTo(1);
		}
	}

	//https://github.com/spring-projects/spring-integration/issues/2569
	@Test
	public void initializeFilterByConstructor() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>(Arrays.asList(new SimplePatternFileListFilter(PATTERN_ANY_TEXT_FILES)))) {
			List<File> result = chain.filterFiles(oneFile);
			assertThat(result.size()).isEqualTo(1);
		}
	}

	private static class MockOldFile extends File {

		private static final long serialVersionUID = 1L;

		MockOldFile(String pathname) {
			super(pathname);
		}

		@Override
		public long lastModified() {
			return 1;
		}

	}

}
