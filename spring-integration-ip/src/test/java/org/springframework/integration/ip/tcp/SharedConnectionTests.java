/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SharedConnectionTests {
	
	@Autowired
	AbstractApplicationContext ctx;
	
	@Autowired
	@Qualifier(value="inboundServer")
	TcpReceivingChannelAdapter listener;
	
	/**
	 * Tests a loopback. The client-side outbound adapter sends a message over
	 * a connection from the client connection factory; the server side 
	 * receives the message, puts in on a channel which is the input channel
	 * for the outbound adapter that's sharing the connections. The response 
	 * comes back to an inbound adapter that is sharing the client's 
	 * connection and we verify we get the echo back as expected.
	 * 
	 * @throws Exception
	 */
	@Test
	public void test1() throws Exception {
		int n = 0;
		while (!listener.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		MessageChannel input = ctx.getBean("input", MessageChannel.class);
		input.send(MessageBuilder.withPayload("Test").build());
		QueueChannel replies = ctx.getBean("replies", QueueChannel.class);
		Message<?> message = replies.receive(10000);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundClient", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("ip:tcp-inbound-channel-adapter", componentHistoryRecord.getProperty("type"));
		assertNotNull(message);
		assertEquals("Test", message.getPayload());
	}

}
