/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class ChannelPublishingJmsMessageListenerTests {

	private final Session session = new StubSession("test");

	@Test
	public void noReplyToAndNoDefault() throws JMSException {
		final QueueChannel requestChannel = new QueueChannel();
		startBackgroundReplier(requestChannel);
		ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener();
		listener.setExpectReply(true);
		listener.setRequestChannel(requestChannel);
		listener.setMessageConverter(new TestMessageConverter());
		jakarta.jms.Message jmsMessage = session.createTextMessage("test");
		listener.setBeanFactory(mock(BeanFactory.class));
		listener.afterPropertiesSet();
		assertThatExceptionOfType(InvalidDestinationException.class)
				.isThrownBy(() -> listener.onMessage(jmsMessage, session));
	}

	@Test
	public void testBadConversion() throws Exception {
		final QueueChannel requestChannel = new QueueChannel();
		ChannelPublishingJmsMessageListener listener = new ChannelPublishingJmsMessageListener();
		LogAccessor logger = spy(TestUtils.getPropertyValue(listener, "logger", LogAccessor.class));
		doNothing().when(logger).error(any(Throwable.class), anyString());
		new DirectFieldAccessor(listener).setPropertyValue("logger", logger);
		listener.setRequestChannel(requestChannel);
		QueueChannel errorChannel = new QueueChannel();
		listener.setErrorChannel(errorChannel);
		listener.setBeanFactory(mock(BeanFactory.class));
		listener.setMessageConverter(new TestMessageConverter() {

			@Override
			public Object fromMessage(jakarta.jms.Message message) throws MessageConversionException {
				return null;
			}

		});
		listener.afterPropertiesSet();
		jakarta.jms.Message jmsMessage = session.createTextMessage("test");
		listener.onMessage(jmsMessage, mock(Session.class));
		ErrorMessage received = (ErrorMessage) errorChannel.receive(0);
		assertThat(received).isNotNull();
		assertThat(received.getPayload().getMessage()).startsWith("Inbound conversion failed");
		listener.stop();
	}

	private void startBackgroundReplier(final PollableChannel channel) {
		new SimpleAsyncTaskExecutor().execute(() -> {
			Message<?> request = channel.receive(50000);
			Message<?> reply = new GenericMessage<>(((String) request.getPayload()).toUpperCase());
			((MessageChannel) request.getHeaders().getReplyChannel()).send(reply, 5000);
		});
	}

	private static class TestMessageConverter implements MessageConverter {

		@Override
		public Object fromMessage(jakarta.jms.Message message) throws MessageConversionException {
			return "test-from";
		}

		@Override
		public jakarta.jms.Message toMessage(Object object, Session session) throws MessageConversionException {
			return new StubTextMessage("test-to");
		}

	}

}
