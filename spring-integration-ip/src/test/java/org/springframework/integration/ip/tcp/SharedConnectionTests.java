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

package org.springframework.integration.ip.tcp;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class SharedConnectionTests {

	@Autowired
	AbstractApplicationContext ctx;

	@Autowired
	AbstractServerConnectionFactory server;

	@Autowired
	AbstractClientConnectionFactory client;

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
		TestingUtilities.waitListening(this.server, null);
		this.client.setPort(this.server.getPort());
		this.ctx.getBeansOfType(ConsumerEndpointFactoryBean.class).values().forEach(c -> c.start());
		MessageChannel input = ctx.getBean("input", MessageChannel.class);
		input.send(MessageBuilder.withPayload("Test").build());
		QueueChannel replies = ctx.getBean("replies", QueueChannel.class);
		Message<?> message = replies.receive(10000);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundClient", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.getProperty("type")).isEqualTo("ip:tcp-inbound-channel-adapter");
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("Test");
	}

}
