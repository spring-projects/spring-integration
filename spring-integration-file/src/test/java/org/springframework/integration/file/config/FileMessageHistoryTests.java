/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
