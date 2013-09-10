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

package org.springframework.integration.handler;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.containsString;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
@RunWith(org.mockito.runners.MockitoJUnitRunner.class)
public class AbstractReplyProducingMessageHandlerTests {

	private AbstractReplyProducingMessageHandler handler = new AbstractReplyProducingMessageHandler() {
		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}
	};

	private Message<?> message = MessageBuilder.withPayload("test").build();

	@Mock
	private MessageChannel channel = null;


	@Test
	public void errorMessageShouldContainChannelName() {
		handler.setOutputChannel(channel);
		when(channel.send(message)).thenReturn(false);
		when(channel.toString()).thenReturn("testChannel");
		try {
			handler.handleMessage(message);
			fail("Expected a MessagingException");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage(), containsString("'testChannel'"));
		}
	}

}
