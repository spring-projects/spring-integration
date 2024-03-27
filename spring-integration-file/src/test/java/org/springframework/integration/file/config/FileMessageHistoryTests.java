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

package org.springframework.integration.file.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class FileMessageHistoryTests {

	@Test
	public void testMessageHistory() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("file-message-history-context.xml", getClass());

		TemporaryFolder input = context.getBean(TemporaryFolder.class);
		File file = input.newFile("FileMessageHistoryTest.txt");
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("hello");
		out.close();

		PollableChannel outChannel = context.getBean("outChannel", PollableChannel.class);
		Message<?> message = outChannel.receive(10000);
		assertThat(message).isNotNull();
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "fileAdapter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("file:inbound-channel-adapter");

		context.close();
	}

}
