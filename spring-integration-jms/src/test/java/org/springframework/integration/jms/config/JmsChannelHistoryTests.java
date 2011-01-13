/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.jms.config;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.integration.Message;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * @author Oleg Zhurakousky
 *
 */
public class JmsChannelHistoryTests {

	@SuppressWarnings("rawtypes")
	@Test
	public void testMessageHistory() throws Exception{
		AbstractMessageListenerContainer mlContainer = mock(AbstractMessageListenerContainer.class);
		JmsTemplate template = mock(JmsTemplate.class);
		SubscribableJmsChannel channel = new SubscribableJmsChannel(mlContainer, template);
		channel.setShouldTrack(true);
		channel.setBeanName("jmsChannel");
		Message<String> message = new GenericMessage<String>("hello");
		
		doAnswer(new Answer() {
		      @SuppressWarnings("unchecked")
			public Object answer(InvocationOnMock invocation) {
		          Message<String> msg = (Message<String>) invocation.getArguments()[0];
		          MessageHistory history = MessageHistory.read(msg);
		  		  assertTrue(history.get(0).contains("jmsChannel"));
		          return null;
		      }})
		  .when(template).convertAndSend(Mockito.any(Message.class));
		channel.send(message);
		verify(template, times(1)).convertAndSend(Mockito.any(Message.class));
	}
}
