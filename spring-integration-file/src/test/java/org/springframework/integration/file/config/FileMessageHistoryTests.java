/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class FileMessageHistoryTests {

	@TempDir
	public static File tempFolder;

	@Autowired
	PollableChannel outChannel;

	@Test
	public void testMessageHistory() throws Exception {
		File file = new File(tempFolder, "FileMessageHistoryTest.txt");
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		out.write("hello");
		out.close();

		Message<?> message = outChannel.receive(10000);
		assertThat(message).isNotNull();
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "fileAdapter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("file:inbound-channel-adapter");

	}

}
