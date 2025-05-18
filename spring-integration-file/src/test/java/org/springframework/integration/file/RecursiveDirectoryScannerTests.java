/*
 * Copyright 2017-2025 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.integration.file.filters.AcceptOnceFileListFilter;

import static org.assertj.core.api.Assertions.assertThat;

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

	@TempDir
	public File recursivePath;

	@BeforeEach
	public void setup() throws IOException {
		this.subFolder = new File(this.recursivePath, "subFolder");
		this.subSubFolder = new File(this.subFolder, "subSubFolder");
		this.subSubFolder.mkdirs();
		this.topLevelFile = new File(this.recursivePath, "file1");
		this.topLevelFile.createNewFile();
		this.subLevelFile = new File(this.subFolder, "file2");
		this.subLevelFile.createNewFile();
		this.subSubLevelFile = new File(this.subSubFolder, "file3");
		this.subSubLevelFile.createNewFile();
	}

	@Test
	public void shouldReturnAllFilesIncludingDirs() throws IOException {
		RecursiveDirectoryScanner scanner = new RecursiveDirectoryScanner();
		scanner.setFilter(new AcceptOnceFileListFilter<>());
		List<File> files = scanner.listFiles(this.recursivePath);
		assertThat(files.size()).isEqualTo(5);
		assertThat(files).contains(this.topLevelFile);
		assertThat(files).contains(this.subLevelFile);
		assertThat(files).contains(this.subSubLevelFile);
		assertThat(files).contains(this.subFolder);
		assertThat(files).contains(this.subSubFolder);
		File file = new File(this.subSubFolder, "file4");
		file.createNewFile();
		files = scanner.listFiles(this.recursivePath);
		assertThat(files.size()).isEqualTo(1);
		assertThat(files).contains(file);
	}

}
