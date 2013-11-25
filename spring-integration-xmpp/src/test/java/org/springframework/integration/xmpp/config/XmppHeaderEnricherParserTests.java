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

package org.springframework.integration.xmpp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.xmpp.XmppHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Josh Long
 * @author Gunnar Hillert
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XmppHeaderEnricherParserTests {

	@Value("#{input}")
	private DirectChannel input;

	@Value("#{output}")
	private DirectChannel output;

	@SuppressWarnings("rawtypes")
	@Test
	public void to() {
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		MessageHandler handler = mock(MessageHandler.class);
		doAnswer(new Answer() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Message message = (Message) invocation.getArguments()[0];
				String chatToUser = (String) message.getHeaders().get(XmppHeaders.TO);
				assertNotNull(chatToUser);
				assertEquals("test1@example.org", chatToUser);
				return null;
			}
		}).when(handler).handleMessage(Mockito.any(Message.class));
		output.subscribe(handler);
		messagingTemplate.send(input, MessageBuilder.withPayload("foo").build());
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
	}

}
