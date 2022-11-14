/*
 * Copyright 2002-2022 the original author or authors.
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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.HeadDirectoryScanner;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class FileInboundChannelAdapterWithQueueSizeTests {

	private static final String PATHNAME = System.getProperty("java.io.tmpdir") + "/"
			+ FileInboundChannelAdapterWithQueueSizeTests.class.getSimpleName();

	private static File inputDir;

	@Autowired
	@Qualifier("inputDirPoller.adapter.source")
	FileReadingMessageSource source1;

	@Autowired
	@Qualifier("inputDirPollerSimpleFilter.adapter.source")
	FileReadingMessageSource source2;

	@BeforeClass
	public static void setupInputDir() {
		inputDir = new File(PATHNAME);
		inputDir.mkdir();
		clean();
	}

	@AfterClass
	public static void tearDown() {
		clean();
		inputDir.delete();
	}

	private static void clean() {
		File[] files = inputDir.listFiles();
		for (File file : files) {
			file.delete();
		}
	}

	@Before
	public void generateTestFiles() throws Exception {
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
		File.createTempFile("test", null, inputDir).setLastModified(System.currentTimeMillis() - 1000);
	}

	@Test
	public void queueSize() {
		HeadDirectoryScanner scanner1 = TestUtils.getPropertyValue(source1, "scanner", HeadDirectoryScanner.class);
		HeadDirectoryScanner scanner2 = TestUtils.getPropertyValue(source2, "scanner", HeadDirectoryScanner.class);
		List<File> files = scanner1.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(2);
		files = scanner2.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(2);
		files.get(0).delete();
		files.get(1).delete();
		files = scanner1.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(1);
		files = scanner2.listFiles(new File(PATHNAME));
		assertThat(files.size()).isEqualTo(1);
	}

}
