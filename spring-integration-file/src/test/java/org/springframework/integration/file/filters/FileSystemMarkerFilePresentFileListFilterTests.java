/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.io.File;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class FileSystemMarkerFilePresentFileListFilterTests {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test() throws Exception {
		FileSystemMarkerFilePresentFileListFilter filter = new FileSystemMarkerFilePresentFileListFilter(
				new SimplePatternFileListFilter("*.txt"));
		File foo = this.folder.newFile("foo.txt");
		foo.createNewFile();
		assertThat(filter.filterFiles(new File[] {foo}).size()).isEqualTo(0);
		File complete = this.folder.newFile("foo.txt.complete");
		complete.createNewFile();
		List<File> filtered = filter.filterFiles(new File[] {foo, complete});
		assertThat(filtered.size()).isEqualTo(1);
		assertThat(filtered.get(0).getName()).isEqualTo("foo.txt");
	}

}
