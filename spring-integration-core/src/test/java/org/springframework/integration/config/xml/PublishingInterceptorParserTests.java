/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PublishingInterceptorParserTests {

	@Autowired
	private TestBean testBean;

	@Autowired
	private DefaultTestBean defaultTestBean;

	@Autowired
	private SubscribableChannel defaultChannel;

	@Autowired
	private SubscribableChannel echoChannel;

	@Test
	public void validateDefaultChannelPublishing() {
		MessageHandler handler = Mockito.mock(MessageHandler.class);
		defaultChannel.subscribe(handler);
		doAnswer(invocation -> {
			Message<?> message = invocation.getArgument(0);
			assertThat(message.getPayload()).isEqualTo("hello");
			return null;
		}).when(handler).handleMessage(any(Message.class));
		testBean.echoDefaultChannel("hello");
		verify(handler, times(1)).handleMessage(any(Message.class));
	}

	@Test
	public void validateEchoChannelPublishing() {
		MessageHandler handler = Mockito.mock(MessageHandler.class);
		echoChannel.subscribe(handler);
		doAnswer(invocation -> {
			Message<?> message = invocation.getArgument(0);
			assertThat(message.getHeaders().get("foo")).isEqualTo("bar");
			assertThat(message.getPayload()).isEqualTo("Echoing: hello");
			return null;
		}).when(handler).handleMessage(any(Message.class));
		testBean.echo("hello");
		verify(handler, times(1)).handleMessage(any(Message.class));
	}

	/**
	 * Need to set 'debug' level
	 */
	@Test
	public void validateNullChannelPublishing() {
		defaultTestBean.echo("hello");
	}

	public static class TestBean {

		public String echo(String str) {
			return str;
		}

		public String echoUpperCase(String str) {
			return str.toUpperCase();
		}

		public String echoDefaultChannel(String str) {
			return str;
		}

	}

	public static class DefaultTestBean {

		public String echo(String str) {
			return str;
		}

	}

}
