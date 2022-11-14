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

package org.springframework.integration.file;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class FileToChannelIntegrationTests {

	@Autowired
	File inputDirectory;

	@Autowired
	PollableChannel fileMessages;

	@Autowired
	PollableChannel resultChannel;

	@Test
	public void fileMessageToChannel() throws Exception {
		File file = File.createTempFile("test", null, inputDirectory);
		file.setLastModified(System.currentTimeMillis() - 1000);

		Message<?> received = this.fileMessages.receive(10000);
		assertThat(received).isNotNull();
		Message<?> result = this.resultChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo(Boolean.TRUE);
		assertThat(!file.exists()).isTrue();
	}

	@Test
	public void directoryExhaustion() throws Exception {
		File.createTempFile("test", null, inputDirectory).setLastModified(System.currentTimeMillis() - 1000);
		Message<?> received = this.fileMessages.receive(10000);
		assertThat(received).isNotNull();
		assertThat(fileMessages.receive(200)).isNull();
	}

}
