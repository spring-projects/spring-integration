/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.message.CommandMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.PollCommand;
import org.springframework.integration.scheduling.PollingSchedule;

/**
 * @author Mark Fisher
 */
public class ByteStreamSourceTests {

	@Test
	public void testEndOfStream() {
		byte[] bytes = new byte[] {1,2,3};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		MessageChannel channel = new QueueChannel();
		ByteStreamSource source = new ByteStreamSource(stream);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel);
		endpoint.afterPropertiesSet();
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message1 = channel.receive(500);
		byte[] payload = (byte[]) message1.getPayload();
		assertEquals(3, payload.length);
		assertEquals(1, payload[0]);
		assertEquals(2, payload[1]);
		assertEquals(3, payload[2]);
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message3 = channel.receive(0);
		assertNull(message3);
	}

	@Test
	public void testByteArrayIsTruncated() {
		byte[] bytes = new byte[] {0,1,2,3,4,5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		MessageChannel channel = new QueueChannel();
		ByteStreamSource source = new ByteStreamSource(stream);
		source.setBytesPerMessage(4);
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel);
		endpoint.afterPropertiesSet();
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message1 = channel.receive(0);
		assertEquals(4, ((byte[]) message1.getPayload()).length);
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message3 = channel.receive(0);
		assertEquals(2, ((byte[]) message3.getPayload()).length);
	}

	@Test
	public void testByteArrayIsNotTruncated() {
		byte[] bytes = new byte[] {0,1,2,3,4,5};
		ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
		MessageChannel channel = new QueueChannel();
		ByteStreamSource source = new ByteStreamSource(stream);
		source.setBytesPerMessage(4);
		source.setShouldTruncate(false);
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel);
		endpoint.afterPropertiesSet();
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message1 = channel.receive(0);
		assertEquals(4, ((byte[]) message1.getPayload()).length);
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message3 = channel.receive(0);
		assertEquals(4, ((byte[]) message3.getPayload()).length);
		assertEquals(0, ((byte[]) message3.getPayload())[3]);
	}

}
