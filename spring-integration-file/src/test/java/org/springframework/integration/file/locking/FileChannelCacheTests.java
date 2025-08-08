/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Roux
 * @since 4.3.22
 */
public class FileChannelCacheTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	@Test
	public void throwsExceptionWhenFileNotExists() throws IOException {
		File testFile = new File(temp.getRoot(), "test0");
		assertThat(testFile.exists()).isFalse();
		assertThat(FileChannelCache.tryLockFor(testFile)).isNull();
		assertThat(testFile.exists()).isFalse();
	}

	@Test
	public void fileLocked() throws IOException {
		File testFile = temp.newFile("test1");
		testFile.createNewFile();
		assertThat(testFile.exists()).isTrue();
		assertThat(FileChannelCache.tryLockFor(testFile)).isNotNull();
		FileChannelCache.closeChannelFor(testFile);
	}

}
