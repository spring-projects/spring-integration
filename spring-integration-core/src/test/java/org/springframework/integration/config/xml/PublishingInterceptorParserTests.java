/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
			assertEquals("hello", message.getPayload());
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
			assertEquals("bar", message.getHeaders().get("foo"));
			assertEquals("Echoing: hello", message.getPayload());
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
