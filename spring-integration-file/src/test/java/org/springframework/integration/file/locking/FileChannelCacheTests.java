/*
 * Copyright 2019-present the original author or authors.
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
import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Roux
 * @since 4.3.22
 */
public class FileChannelCacheTests {

	@TempDir
	public File temp;

	@Test
	public void throwsExceptionWhenFileNotExists() throws IOException {
		File testFile = new File(temp, "test0");
		assertThat(testFile.exists()).isFalse();
		assertThat(FileChannelCache.tryLockFor(testFile)).isNull();
		assertThat(testFile.exists()).isFalse();
	}

	@Test
	public void fileLocked() throws IOException {
		File testFile = new File(temp, "test1");
		testFile.createNewFile();
		assertThat(testFile.exists()).isTrue();
		assertThat(FileChannelCache.tryLockFor(testFile)).isNotNull();
		FileChannelCache.closeChannelFor(testFile);
	}

}
