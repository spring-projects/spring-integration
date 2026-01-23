/*
 * Copyright 2002-present the original author or authors.
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
