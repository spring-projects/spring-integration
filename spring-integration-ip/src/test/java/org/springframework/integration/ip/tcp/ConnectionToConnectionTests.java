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

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 * @since 2.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectionToConnectionTests {
	
	@Autowired
	AbstractApplicationContext ctx;
	
	@Autowired
	private AbstractClientConnectionFactory client;
	
	@Autowired
	private AbstractServerConnectionFactory server;
	
	private TcpReceivingChannelAdapter receiver;
	
	@Before
	public void setup() {
		receiver = new TcpReceivingChannelAdapter();
		server.registerListener(receiver);
		ctx.start();
	}
	
	@Test
	public void testConnect() throws Exception {
		int n = 0;
		while (!server.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("Failed to listen");
			}
		}
		TcpConnection connection = client.getConnection();
		QueueChannel channel = new QueueChannel();
		receiver.setOutputChannel(channel);
		connection.send(MessageBuilder.withPayload("Test").build());
		Message<?> m = channel.receive(10000);
		assertNotNull(m);
	}

}
