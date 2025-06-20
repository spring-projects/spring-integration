/*
 * Copyright 2017-present the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class FileSystemMarkerFilePresentFileListFilterTests {

	@TempDir
	public File folder;

	@Test
	public void test() throws Exception {
		FileSystemMarkerFilePresentFileListFilter filter = new FileSystemMarkerFilePresentFileListFilter(
				new SimplePatternFileListFilter("*.txt"));
		File foo = new File(folder, "foo.txt");
		foo.createNewFile();
		assertThat(filter.filterFiles(new File[] {foo}).size()).isEqualTo(0);
		File complete = new File(folder, "foo.txt.complete");
		complete.createNewFile();
		List<File> filtered = filter.filterFiles(new File[] {foo, complete});
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0).getName()).isEqualTo("foo.txt");
	}

}
