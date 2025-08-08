/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Emmanuel Roux
 */
public class NioFileLockerTests {

	private File workdir;

	@Rule
	public TemporaryFolder temp = new TemporaryFolder() {

		@Override
		public void create() throws IOException {
			super.create();
			workdir = temp.newFolder(NioFileLockerTests.class.getSimpleName());
		}
	};

	@Test
	public void fileListedByFirstFilter() throws Exception {
		NioFileLocker filter = new NioFileLocker();
		File testFile = new File(workdir, "test0");
		testFile.createNewFile();
		assertThat(filter.filterFiles(workdir.listFiles()).get(0)).isEqualTo(testFile);
		filter.lock(testFile);
		assertThat(filter.filterFiles(workdir.listFiles()).get(0)).isEqualTo(testFile);
		filter.unlock(testFile);
		Field channelCache = FileChannelCache.class.getDeclaredField("channelCache");
		channelCache.setAccessible(true);
		assertThat(((Map<?, ?>) channelCache.get(null))).isEmpty();
		assertThat(((Map<?, ?>) TestUtils.getPropertyValue(filter, "lockCache", Map.class))).isEmpty();
	}

	@Test
	public void fileNotListedWhenLockedByOtherFilter() throws IOException {
		NioFileLocker filter1 = new NioFileLocker();
		FileListFilter<File> filter2 = new NioFileLocker();
		File testFile = new File(workdir, "test1");
		testFile.createNewFile();
		assertThat(filter1.filterFiles(workdir.listFiles()).get(0)).isEqualTo(testFile);
		filter1.lock(testFile);
		assertThat(filter2.filterFiles(workdir.listFiles())).isEqualTo(new ArrayList<File>());
		filter1.unlock(testFile);
	}

	@Test
	public void fileLockedWhenNotAlreadyLockedAndExists() throws IOException {
		NioFileLocker locker = new NioFileLocker();
		File testFile = new File(workdir, "test2");
		testFile.createNewFile();
		assertThat(locker.lock(testFile)).isTrue();
		locker.unlock(testFile);
	}

	@Test
	public void fileNotLockedWhenNotExists() throws IOException {
		NioFileLocker locker = new NioFileLocker();
		File testFile = new File(workdir, "test3");
		assertThat(locker.lock(testFile)).isFalse();
		assertThat(testFile.exists()).isFalse();
	}

}
