/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderEnricherTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void replyChannel() {
		PollableChannel replyChannel = context.getBean("testReplyChannel", PollableChannel.class);
		MessageChannel inputChannel = context.getBean("replyChannelInput", MessageChannel.class);
		inputChannel.send(new GenericMessage<String>("test"));
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		assertEquals(replyChannel, result.getHeaders().getReplyChannel());
	}

	@Test
	public void errorChannel() {
		PollableChannel errorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		MessageChannel inputChannel = context.getBean("errorChannelInput", MessageChannel.class);
		inputChannel.send(new GenericMessage<String>("test"));
		Message<?> errorMessage = errorChannel.receive(1000);
		assertNotNull(errorMessage);
		Object errorPayload = errorMessage.getPayload();
		assertEquals(MessageTransformationException.class, errorPayload.getClass());
		Message<?> failedMessage = ((MessageTransformationException) errorPayload).getFailedMessage();
		assertEquals("test", failedMessage.getPayload());
		assertEquals(errorChannel, failedMessage.getHeaders().getErrorChannel());
	}

	@Test
	public void correlationIdValue() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdValueInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals("ABC", result.getHeaders().getCorrelationId());
	}

	@Test
	public void correlationIdValueWithType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdValueWithTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		Object correlationId = result.getHeaders().getCorrelationId();
		assertEquals(Long.class, correlationId.getClass());
		assertEquals(new Long(123), correlationId);
	}

	@Test
	public void correlationIdRef() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdRefInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(new Integer(123), result.getHeaders().getCorrelationId());
	}

	@Test
	public void expirationDateValue() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expirationDateValueInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(new Long(1111), result.getHeaders().getExpirationDate());
	}

	@Test
	public void expirationDateRef() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expirationDateRefInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(new Long(9999), result.getHeaders().getExpirationDate());
	}

	@Test
	public void priority() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("priorityInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(new Integer(42), result.getHeaders().getPriority());
	}

	@Test
	public void expressionUsingPayload() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("payloadExpressionInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<TestBean>(new TestBean("foo")));
		assertNotNull(result);
		assertEquals("foobar", result.getHeaders().get("testHeader"));
	}

	@Test
	public void expressionUsingHeader() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("headerExpressionInput", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeader1", "foo").build();
		Message<?> result = template.sendAndReceive(channel, message);
		assertNotNull(result);
		assertEquals("foobar", result.getHeaders().get("testHeader2"));
	}

	@Test
	public void expressionWithDateType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expressionWithDateTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		Object headerValue = result.getHeaders().get("currentDate");
		assertEquals(Date.class, headerValue.getClass());
		Date date = (Date) headerValue;
		assertTrue(new Date().getTime() - date.getTime() < 1000);
	}

	@Test
	public void expressionWithLongType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expressionWithLongTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(Long.class, result.getHeaders().get("number").getClass());
		assertEquals(new Long(12345), result.getHeaders().get("number"));
	}

	@Test
	public void refWithMethod() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("refWithMethod", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(String.class, result.getHeaders().get("testHeader").getClass());
		assertEquals("testBeanForMethodInvoker", result.getHeaders().get("testHeader"));
	}

	@Test
	public void ref() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("ref", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(TestBean.class, result.getHeaders().get("testHeader").getClass());
		TestBean testBeanForRef = context.getBean("testBean1", TestBean.class);
		assertSame(testBeanForRef, result.getHeaders().get("testHeader"));
	}

	@Test
	public void innerBean() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("innerBean", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(TestBean.class, result.getHeaders().get("testHeader").getClass());
		TestBean testBeanForInnerBean = new TestBean("testBeanForInnerBean");
		assertEquals(testBeanForInnerBean, result.getHeaders().get("testHeader"));
	}

	@Test
	public void innerBeanWithMethod() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("innerBeanWithMethod", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<String>("test"));
		assertNotNull(result);
		assertEquals(String.class, result.getHeaders().get("testHeader").getClass());
		assertEquals("testBeanForInnerBeanWithMethod", result.getHeaders().get("testHeader"));
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testFailConfigUnexpectedSubElement() {
		new ClassPathXmlApplicationContext("HeaderEnricherWithUnexpectedSubElementForHeader-fail-context.xml", this.getClass());
	}

	public static class TestBean {

		private final String name;

		TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			TestBean testBean = (TestBean) o;

			if (name != null ? !name.equals(testBean.name) : testBean.name != null) return false;

			return true;
		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}
	}

}
