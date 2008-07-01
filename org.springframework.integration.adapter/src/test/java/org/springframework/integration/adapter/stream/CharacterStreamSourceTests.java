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

import java.io.StringReader;

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
public class CharacterStreamSourceTests {

	@Test
	public void testEndOfStream() {
		StringReader reader = new StringReader("test");
		MessageChannel channel = new QueueChannel();
		CharacterStreamSource source = new CharacterStreamSource(reader);
		PollingSchedule schedule = new PollingSchedule(1000);
		schedule.setInitialDelay(10000);
		SourceEndpoint endpoint = new SourceEndpoint(source, channel);
		endpoint.afterPropertiesSet();
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message1 = channel.receive(0);
		assertEquals("test", message1.getPayload());
		Message<?> message2 = channel.receive(0);
		assertNull(message2);
		endpoint.invoke(new CommandMessage(new PollCommand()));
		Message<?> message3 = channel.receive(0);
		assertNull(message3);
	}

}
