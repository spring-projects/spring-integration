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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class AbstractMarkerFilePresentFileListFilterTests {

	@Test
	public void testDefault() {
		StringMarkerFilePresentFileListFilter filter = new StringMarkerFilePresentFileListFilter(
				new StringSimplePatternFilter("*.txt"));
		List<String> filtered = filter.filterFiles(new String[] {"foo.txt", "foo.txt.complete", "bar.txt"});
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0)).isEqualTo("foo.txt");
	}

	@Test
	public void testSimple() {
		StringMarkerFilePresentFileListFilter filter = new StringMarkerFilePresentFileListFilter(
				new StringSimplePatternFilter("*.txt"), ".done");
		List<String> filtered = filter.filterFiles(new String[] {"foo.txt", "foo.txt.done", "bar.txt", "baz.txt"});
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0)).isEqualTo("foo.txt");
	}

	@Test
	public void testCustomFunction() {
		StringMarkerFilePresentFileListFilter filter = new StringMarkerFilePresentFileListFilter(
				new StringSimplePatternFilter("*.txt"), s -> "allFilesDone");
		List<String> filtered = filter.filterFiles(new String[] {"foo.txt", "bar.txt"});
		assertThat(filtered.size()).isEqualTo(0);
		filtered = filter.filterFiles(new String[] {"foo.txt", "bar.txt", "allFilesDone"});
		assertThat(filtered.get(0)).isEqualTo("foo.txt");
		assertThat(filtered.get(1)).isEqualTo("bar.txt");
	}

	@Test
	public void testMulti() {
		Map<FileListFilter<String>, Function<String, String>> map = new HashMap<>();
		map.put(new StringSimplePatternFilter("*.txt"),
				AbstractMarkerFilePresentFileListFilter.defaultFileNameFunction(".done"));
		map.put(new StringSimplePatternFilter("*.xml"),
				AbstractMarkerFilePresentFileListFilter.defaultFileNameFunction(".complete"));
		StringMarkerFilePresentFileListFilter filter = new StringMarkerFilePresentFileListFilter(map);
		List<String> filtered = filter
				.filterFiles(new String[] {"foo.txt", "foo.txt.done", "bar.xml", "bar.xml.complete", "baz.txt"});
		assertThat(filtered.size()).isEqualTo(2);
		assertThat(filtered.get(0)).isEqualTo("foo.txt");
		assertThat(filtered.get(1)).isEqualTo("bar.xml");
	}

	private static class StringSimplePatternFilter extends AbstractSimplePatternFileListFilter<String> {

		StringSimplePatternFilter(String pattern) {
			super(pattern);
		}

		@Override
		protected String getFilename(String file) {
			return file;
		}

		@Override
		protected boolean isDirectory(String file) {
			return false;
		}

	}

	private static class StringMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<String> {

		StringMarkerFilePresentFileListFilter(FileListFilter<String> filter) {
			super(filter);
		}

		StringMarkerFilePresentFileListFilter(FileListFilter<String> filter, String suffix) {
			super(filter, suffix);
		}

		StringMarkerFilePresentFileListFilter(FileListFilter<String> filter,
				Function<String, String> function) {
			super(filter, function);
		}

		StringMarkerFilePresentFileListFilter(
				Map<FileListFilter<String>, Function<String, String>> filtersAndFunctions) {
			super(filtersAndFunctions);
		}

		@Override
		protected String getFilename(String file) {
			return file;
		}

	}

}
