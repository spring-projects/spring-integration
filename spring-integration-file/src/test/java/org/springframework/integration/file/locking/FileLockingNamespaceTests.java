/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artme Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FileLockingNamespaceTests {

	@Autowired
	@Qualifier("nioLockingAdapter.adapter")
	SourcePollingChannelAdapter nioAdapter;

	FileReadingMessageSource nioLockingSource;

	@Autowired
	@Qualifier("customLockingAdapter.adapter")
	SourcePollingChannelAdapter customAdapter;

	FileReadingMessageSource customLockingSource;

	@Before
	public void extractSources() {
		nioLockingSource = (FileReadingMessageSource) new DirectFieldAccessor(nioAdapter).getPropertyValue("source");
		customLockingSource =
				(FileReadingMessageSource) new DirectFieldAccessor(customAdapter).getPropertyValue("source");
	}

	@Test
	public void shouldLoadConfig() {
		//verify Spring can load the configuration
	}

	@Test
	public void shouldSetCustomLockerProperly() {
		assertThat(extractFromScanner("locker", customLockingSource)).isInstanceOf(StubLocker.class);
		assertThat(extractFromScanner("filter", customLockingSource)).isInstanceOf(CompositeFileListFilter.class);
	}

	private Object extractFromScanner(String propertyName, FileReadingMessageSource source) {
		return new DirectFieldAccessor(new DirectFieldAccessor(source).getPropertyValue("scanner"))
				.getPropertyValue(propertyName);
	}

	@Test
	public void shouldSetNioLockerProperly() {
		assertThat(extractFromScanner("locker", nioLockingSource)).isInstanceOf(NioFileLocker.class);
		assertThat(extractFromScanner("filter", nioLockingSource)).isInstanceOf(CompositeFileListFilter.class);
	}

	public static class StubLocker extends AbstractFileLockerFilter {

		public boolean lock(File fileToLock) {
			return true;
		}

		public boolean isLockable(File file) {
			return true;
		}

		public void unlock(File fileToUnlock) {
			//
		}

	}

}
