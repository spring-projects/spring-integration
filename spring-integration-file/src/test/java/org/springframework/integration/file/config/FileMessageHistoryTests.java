/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.config;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.test.util.TestUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
public class FileMessageHistoryTests {


	@Test
	public void testMessageHistory() throws Exception{
		ApplicationContext context = new ClassPathXmlApplicationContext("file-message-history-context.xml", this.getClass());
		TemporaryFolder input = context.getBean(TemporaryFolder.class);
		File file = input.newFile("FileMessageHistoryTest.txt");
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
	    out.write("hello");
	    out.close();

	    PollableChannel outChannel =  context.getBean("outChannel", PollableChannel.class);
	    Message<?> message = outChannel.receive(1000);
		assertThat(message, is(notNullValue()));
	    MessageHistory history = MessageHistory.read(message);
	    assertThat(history, is(notNullValue()));
	    Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "fileAdapter", 0);
	    assertNotNull(componentHistoryRecord);
	    assertEquals("file:inbound-channel-adapter", componentHistoryRecord.get("type"));
	}
}
