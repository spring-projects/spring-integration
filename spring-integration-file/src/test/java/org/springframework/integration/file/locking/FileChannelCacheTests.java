/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * @author Emmanuel Roux
 */
public class FileChannelCacheTests {

	private File workdir;

	@Rule
	public TemporaryFolder temp = new TemporaryFolder() {

		@Override
		public void create() throws IOException {
			super.create();
			workdir = temp.newFolder(FileChannelCacheTests.class.getSimpleName());
		}
	};

	@Test(expected = FileNotFoundException.class)
	public void throwsExceptionWhenFileNotExists() throws IOException {
		File testFile = new File(workdir, "test0");
		assertThat(testFile.exists()).isFalse();
		FileChannelCache.tryLockFor(testFile);
	}

	@Test
	public void fileLocked() throws IOException {
		File testFile = new File(workdir, "test1");
		testFile.createNewFile();
		assertThat(testFile.exists()).isTrue();
		assertThat(FileChannelCache.tryLockFor(testFile)).isNotNull();
		FileChannelCache.closeChannelFor(testFile);
	}
}
