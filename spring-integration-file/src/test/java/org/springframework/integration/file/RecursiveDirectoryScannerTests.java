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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.integration.file.filters.AcceptOnceFileListFilter;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class RecursiveDirectoryScannerTests {

	private File subFolder;

	private File subSubFolder;

	private File topLevelFile;

	private File subLevelFile;

	private File subSubLevelFile;

	@Rule
	public TemporaryFolder recursivePath = new TemporaryFolder();

	@Before
	public void setup() throws IOException {
		this.subFolder = this.recursivePath.newFolder("subFolder");
		this.subSubFolder = new File(this.subFolder, "subSubFolder");
		this.subSubFolder.mkdir();
		this.topLevelFile = this.recursivePath.newFile("file1");
		this.subLevelFile = new File(this.subFolder, "file2");
		this.subLevelFile.createNewFile();
		this.subSubLevelFile = new File(this.subSubFolder, "file3");
		this.subSubLevelFile.createNewFile();
	}

	@Test
	public void shouldReturnAllFilesIncludingDirs() throws IOException {
		RecursiveDirectoryScanner scanner = new RecursiveDirectoryScanner();
		scanner.setFilter(new AcceptOnceFileListFilter<>());
		List<File> files = scanner.listFiles(this.recursivePath.getRoot());
		assertEquals(5, files.size());
		assertThat(files, hasItem(this.topLevelFile));
		assertThat(files, hasItem(this.subLevelFile));
		assertThat(files, hasItem(this.subSubLevelFile));
		assertThat(files, hasItem(this.subFolder));
		assertThat(files, hasItem(this.subSubFolder));
		File file = new File(this.subSubFolder, "file4");
		file.createNewFile();
		files = scanner.listFiles(this.recursivePath.getRoot());
		assertEquals(1, files.size());
		assertThat(files, hasItem(file));
	}

}
