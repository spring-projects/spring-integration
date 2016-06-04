/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.mail.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.mail.MailSendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.mail.MailSender;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 */
public class MailOutboundChannelAdapterParserTests {

	public static volatile int adviceCalled;

	@Test
	public void adapterWithMailSenderReference() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"mailOutboundChannelAdapterParserTests.xml", this.getClass());
		Object adapter = context.getBean("adapterWithMailSenderReference.adapter");
		MailSendingMessageHandler handler = (MailSendingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(handler);
		MailSender mailSender = (MailSender) fieldAccessor.getPropertyValue("mailSender");
		assertNotNull(mailSender);
		assertEquals(23, fieldAccessor.getPropertyValue("order"));
		context.close();
	}

	@Test
	public void advised() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"mailOutboundChannelAdapterParserTests.xml", this.getClass());
		Object adapter = context.getBean("advised.adapter");
		MessageHandler handler = (MessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
		context.close();
	}

	@Test
	public void adapterWithHostProperty() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"mailOutboundChannelAdapterParserTests.xml", this.getClass());
		Object adapter = context.getBean("adapterWithHostProperty.adapter");
		MailSendingMessageHandler handler = (MailSendingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(handler);
		MailSender mailSender = (MailSender) fieldAccessor.getPropertyValue("mailSender");
		assertNotNull(mailSender);
		context.close();
	}

	@Test
	public void adapterWithPollableChannel() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"mailOutboundChannelAdapterParserTests.xml", this.getClass());
		PollingConsumer pc = context.getBean("adapterWithPollableChannel", PollingConsumer.class);
		QueueChannel pollableChannel = TestUtils.getPropertyValue(pc, "inputChannel", QueueChannel.class);
		assertEquals("pollableChannel", pollableChannel.getComponentName());
		context.close();
	}

	@Test
	public void adapterWithJavaMailProperties() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"MailOutboundWithJavamailProperties-context.xml", this.getClass());
		Object adapter = context.getBean("adapterWithHostProperty.adapter");
		MailSendingMessageHandler handler = (MailSendingMessageHandler)
				new DirectFieldAccessor(adapter).getPropertyValue("handler");
		DirectFieldAccessor fieldAccessor = new DirectFieldAccessor(handler);
		MailSender mailSender = (MailSender) fieldAccessor.getPropertyValue("mailSender");
		assertNotNull(mailSender);
		Properties javaMailProperties = (Properties) TestUtils.getPropertyValue(mailSender, "javaMailProperties");
		assertEquals(7, javaMailProperties.size());
		assertNotNull(javaMailProperties);
		assertEquals("true", javaMailProperties.get("mail.smtps.auth"));
		context.close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
