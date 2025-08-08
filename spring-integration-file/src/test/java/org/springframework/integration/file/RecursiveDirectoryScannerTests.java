/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
		assertThat(files.size()).isEqualTo(5);
		assertThat(files).contains(this.topLevelFile);
		assertThat(files).contains(this.subLevelFile);
		assertThat(files).contains(this.subSubLevelFile);
		assertThat(files).contains(this.subFolder);
		assertThat(files).contains(this.subSubFolder);
		File file = new File(this.subSubFolder, "file4");
		file.createNewFile();
		files = scanner.listFiles(this.recursivePath.getRoot());
		assertThat(files.size()).isEqualTo(1);
		assertThat(files).contains(file);
	}

}
