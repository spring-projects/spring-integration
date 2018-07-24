/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.file.filters;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alen Turkovic
 */
public class RecursiveFileListFilterTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test() throws IOException {
		final RecursiveFileListFilter filter = new RecursiveFileListFilter(
				new SimplePatternFileListFilter("matching*"));

		final File sub1 = this.folder.newFolder("matching-folder");
		final File sub2 = this.folder.newFolder("matching-folder", "sub2");
		final File matchingSub2 = this.folder.newFolder("matching-folder", "matching-sub2");
		this.folder.newFile("foo");
		this.folder.newFile("matching-a");

		sub1.toPath()
				.resolve("bar")
				.toFile()
				.createNewFile();
		sub1.toPath()
				.resolve("matching-b")
				.toFile()
				.createNewFile();

		sub2.toPath()
				.resolve("qux")
				.toFile()
				.createNewFile();
		sub2.toPath()
				.resolve("matching-c")
				.toFile()
				.createNewFile();

		matchingSub2.toPath()
				.resolve("baz")
				.toFile()
				.createNewFile();
		matchingSub2.toPath()
				.resolve("matching-d")
				.toFile()
				.createNewFile();

		final List<String> files = filter.filterFiles(this.folder.getRoot().listFiles()).stream()
				.map(File::getName)
				.collect(Collectors.toList());

		assertThat(files).containsExactlyInAnyOrder("matching-a", "matching-b", "matching-c", "matching-d");
	}

}
