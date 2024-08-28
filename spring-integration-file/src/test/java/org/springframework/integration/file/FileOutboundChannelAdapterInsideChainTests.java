/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 */
@SpringJUnitConfig
@DirtiesContext
public class FileOutboundChannelAdapterInsideChainTests {

	static final String TEST_FILE_NAME = FileOutboundChannelAdapterInsideChainTests.class.getSimpleName();

	static final String SAMPLE_CONTENT = "test";

	@TempDir
	static File WORK_DIR;

	public static Properties placeholderProperties = new Properties();

	@Autowired
	private MessageChannel outboundChainChannel;

	@BeforeAll
	static void setupClass() {
		placeholderProperties.put("test.file", TEST_FILE_NAME);
		placeholderProperties.put("work.dir", "'file://" + WORK_DIR.getAbsolutePath() + '\'');
	}

	@Test
	void testFileOutboundChannelAdapterWithinChain() throws IOException {
		Message<String> message = MessageBuilder.withPayload(SAMPLE_CONTENT).build();
		outboundChainChannel.send(message);
		File testFile = new File(WORK_DIR, TEST_FILE_NAME);
		assertThat(testFile.exists()).isTrue();
		byte[] testFileContent = FileCopyUtils.copyToByteArray(testFile);
		assertThat(SAMPLE_CONTENT).isEqualTo(new String(testFileContent));
	}

}
