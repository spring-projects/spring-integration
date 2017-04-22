/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.file;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.springframework.integration.file.filters.ChainFileListFilter;
import org.springframework.integration.file.filters.LastModifiedFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;

/**
 * INT-4216
 *
 * @author Aaron Grant
 * @since 4.3.8
 */
public class ChainFileListFilterIntegrationTests {

	private class MockOldFile extends File {
		private static final long serialVersionUID = 1L;

		MockOldFile(String pathname) {
			super(pathname);
		}

		@Override
		public long lastModified() {
			return 1;
		}
	}

	private File[] noFiles = new File[0];
	private File[] oneFile = new File[] { new MockOldFile("file.txt") };

	@Test
	public void singleModifiedFilterNoFiles() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(noFiles);
			assertEquals(0, result.size());
		}
	}

	@Test
	public void singlePatternFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void singleModifiedFilter() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void patternThenModifiedFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			chain.addFilter(new LastModifiedFileListFilter());
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

	@Test
	public void modifiedThenPatternFilters() throws IOException {
		try (ChainFileListFilter<File> chain = new ChainFileListFilter<>()) {
			chain.addFilter(new LastModifiedFileListFilter());
			chain.addFilter(new SimplePatternFileListFilter("*.txt"));
			List<File> result = chain.filterFiles(oneFile);
			assertEquals(1, result.size());
		}
	}

}
