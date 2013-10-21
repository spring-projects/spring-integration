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
package org.springframework.integration.jms.config;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
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

	@Test
	public void testFullConfig() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext ac = new ClassPathXmlApplicationContext("JmsChannelHistoryTests-context.xml", this.getClass());
		SubscribableChannel channel = ac.getBean("jmsChannel", SubscribableChannel.class);
		PollableChannel resultChannel = ac.getBean("resultChannel", PollableChannel.class);
		channel.send(new GenericMessage<String>("hello"));
		Message<?> resultMessage = resultChannel.receive(5000);
		MessageHistory history = MessageHistory.read(resultMessage);
 		assertTrue(history.get(0).contains("jmsChannel"));
	}
}
