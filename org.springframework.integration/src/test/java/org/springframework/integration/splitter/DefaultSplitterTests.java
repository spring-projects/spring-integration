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

package org.springframework.integration.splitter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class DefaultSplitterTests {

	@Test
	public void splitMessageWithArrayPayload() throws Exception {
		String[] payload = new String[] { "x", "y", "z" };
		Message<String[]> message = MessageBuilder.withPayload(payload).build();
		QueueChannel replyChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);
		splitter.onMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		assertEquals(3, replies.size());
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("x", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("y", reply2.getPayload());
		Message<?> reply3 = replies.get(2);
		assertNotNull(reply3);
		assertEquals("z", reply3.getPayload());
	}

	@Test
	public void splitMessageWithCollectionPayload() throws Exception {
		List<String> payload = Arrays.asList(new String[] { "x", "y", "z" });
		Message<List<String>> message = MessageBuilder.withPayload(payload).build();
		QueueChannel replyChannel = new QueueChannel();
		DefaultMessageSplitter splitter = new DefaultMessageSplitter();
		splitter.setOutputChannel(replyChannel);
		splitter.onMessage(message);
		List<Message<?>> replies = replyChannel.clear();
		assertEquals(3, replies.size());
		Message<?> reply1 = replies.get(0);
		assertNotNull(reply1);
		assertEquals("x", reply1.getPayload());
		Message<?> reply2 = replies.get(1);
		assertNotNull(reply2);
		assertEquals("y", reply2.getPayload());
		Message<?> reply3 = replies.get(2);
		assertNotNull(reply3);
		assertEquals("z", reply3.getPayload());
	}

}
