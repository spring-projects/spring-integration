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

package org.springframework.integration.file.locking;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class FileLockingWithMultipleSourcesIntegrationTests {

	private static File workdir;

	@BeforeClass
	public static void setupWorkDirectory() {
		workdir = new File(new File(System.getProperty("java.io.tmpdir")),
				FileLockingWithMultipleSourcesIntegrationTests.class.getSimpleName());
		workdir.mkdir();
	}

	@Autowired
	@Qualifier("fileSource1")
	private FileReadingMessageSource fileSource1;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource2;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource3;

	@Before
	public void cleanoutWorkDir() {
		for (File file : workdir.listFiles()) {
			file.delete();
		}
	}

	@Test
	public void filePickedUpOnceWithDistinctFilters() throws IOException {
		File testFile = new File(workdir, "test");
		testFile.createNewFile();
		assertThat(this.fileSource1.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		assertThat(this.fileSource2.receive()).isNull();
		FileChannelCache.closeChannelFor(testFile);
	}

	@Test
	public void filePickedUpTwiceWithSharedFilter() throws Exception {
		File testFile = new File(workdir, "test");
		testFile.createNewFile();
		assertThat(this.fileSource1.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		assertThat(this.fileSource3.receive())
				.isNotNull()
				.extracting(Message::getPayload)
				.isEqualTo(testFile);
		FileChannelCache.closeChannelFor(testFile);
	}

}
