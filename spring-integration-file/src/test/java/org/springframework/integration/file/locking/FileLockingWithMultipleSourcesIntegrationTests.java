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
import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.inbound.FileReadingMessageSource;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class FileLockingWithMultipleSourcesIntegrationTests {

	@TempDir
	public static File workdir;

	@Autowired
	@Qualifier("fileSource1")
	private FileReadingMessageSource fileSource1;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource2;

	@Autowired
	@Qualifier("fileSource2")
	private FileReadingMessageSource fileSource3;

	@BeforeEach
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
