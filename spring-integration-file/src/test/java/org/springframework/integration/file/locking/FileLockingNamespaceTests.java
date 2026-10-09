/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.locking;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FileLockingNamespaceTests {

	@TempDir
	public static File tempDir;

	@Autowired
	@Qualifier("nioLockingAdapter.adapter")
	SourcePollingChannelAdapter nioAdapter;

	FileReadingMessageSource nioLockingSource;

	@Autowired
	@Qualifier("customLockingAdapter.adapter")
	SourcePollingChannelAdapter customAdapter;

	FileReadingMessageSource customLockingSource;

	@BeforeEach
	public void extractSources() {
		this.nioLockingSource = (FileReadingMessageSource) this.nioAdapter.getMessageSource();
		this.customLockingSource = (FileReadingMessageSource) this.customAdapter.getMessageSource();
	}

	@Test
	public void shouldLoadConfig() {
		//verify Spring can load the configuration
	}

	@Test
	public void shouldSetCustomLockerProperly() {
		assertThat(TestUtils.<Object>getPropertyValue(this.customLockingSource, "scanner.locker"))
				.isInstanceOf(StubLocker.class);
		assertThat(TestUtils.<Object>getPropertyValue(this.customLockingSource, "scanner.filter"))
				.isInstanceOf(CompositeFileListFilter.class);
	}

	@Test
	public void shouldSetNioLockerProperly() {
		assertThat(TestUtils.<Object>getPropertyValue(this.nioLockingSource, "scanner.locker"))
				.isInstanceOf(NioFileLocker.class);
		assertThat(TestUtils.<Object>getPropertyValue(this.nioLockingSource, "scanner.filter"))
				.isInstanceOf(CompositeFileListFilter.class);
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
