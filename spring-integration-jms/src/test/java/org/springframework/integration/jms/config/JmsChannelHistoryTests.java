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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.SubscribableJmsChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Oleg Zhurakousky
 *
 */
public class JmsChannelHistoryTests {

	@Test
	public void testMessageHistory() throws Exception{
		ActiveMqTestUtils.prepare();
		ApplicationContext ac = new ClassPathXmlApplicationContext("JmsChannelHistory-context.xml", this.getClass());
		SubscribableJmsChannel jmsChannel = ac.getBean("jmsChannel", SubscribableJmsChannel.class);
		JmsService service = ac.getBean("subscriber", JmsService.class);
		
		jmsChannel.send(new GenericMessage<String>("hello"));
		Thread.sleep(5000);
		verify(service, times(1)).handleMessage(Mockito.any(Message.class));
	}
	
	public static interface JmsGateway{
		public Long echo(String time);
	}
	
	public static class JmsService implements MessageHandler{
		public void handleMessage(Message<?> message) throws MessagingException {
			MessageHistory history = MessageHistory.read(message);
			assertTrue(history.contains("jmsChannel"));
		}
	}
}
