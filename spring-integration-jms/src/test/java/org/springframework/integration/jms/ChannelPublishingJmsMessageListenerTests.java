/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jms;

import static org.mockito.Mockito.mock;

import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Session;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ChannelPublishingJmsMessageListenerTests {

	private final Session session = new StubSession("test");


	@Test(expected = InvalidDestinationException.class)
	public void noReplyToAndNoDefault() throws JMSException {
		final QueueChannel requestChannel = new QueueChannel();
		this.startBackgroundReplier(requestChannel);
		ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener();
		listener.setExpectReply(true);
		listener.setRequestChannel(requestChannel);
		listener.setMessageConverter(new TestMessageConverter());
		javax.jms.Message jmsMessage = session.createTextMessage("test");
		listener.setBeanFactory(mock(BeanFactory.class));
		listener.afterPropertiesSet();
		listener.onMessage(jmsMessage, session);
	}

	private void startBackgroundReplier(final PollableChannel channel) {
		new SimpleAsyncTaskExecutor().execute(new Runnable() {
			@Override
			public void run() {
				Message<?> request = channel.receive(5000);
				Message<?> reply = new GenericMessage<String>(((String) request.getPayload()).toUpperCase());
				((MessageChannel) request.getHeaders().getReplyChannel()).send(reply, 5000);
			}
		});
	}

	private static class TestMessageConverter implements MessageConverter {

		@Override
		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			return "test-from";
		}

		@Override
		public javax.jms.Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
			return new StubTextMessage("test-to");
		}
	}

}
