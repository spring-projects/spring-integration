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

package org.springframework.integration.file.config;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
public class FileInboundChannelAdapterWithQueueSizeTests {

	@TempDir
	private static File inputDir;

	@Autowired
	@Qualifier("inputDirPoller.adapter.source")
	FileReadingMessageSource source1;

	@Autowired
	@Qualifier("inputDirPollerSimpleFilter.adapter.source")
	FileReadingMessageSource source2;

	@BeforeEach
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@Test
	public void queueSize() {
		HeadDirectoryScanner scanner1 = TestUtils.getPropertyValue(source1, "scanner");
		HeadDirectoryScanner scanner2 = TestUtils.getPropertyValue(source2, "scanner");
		List<File> files = scanner1.listFiles(inputDir);
		assertThat(files).hasSize(2);
		files = scanner2.listFiles(inputDir);
		assertThat(files).hasSize(2);
		files.get(0).delete();
		files.get(1).delete();
		files = scanner1.listFiles(inputDir);
		assertThat(files).hasSize(1);
		files = scanner2.listFiles(inputDir);
		assertThat(files).hasSize(1);
	}

}
