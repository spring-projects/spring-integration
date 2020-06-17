/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.routingslip.ExpressionEvaluatingRoutingSlipRouteStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.transformer.MessageTransformationException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
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
		inputChannel.send(new GenericMessage<>("test"));
		Message<?> result = replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test //INT-2316
	public void replyChannelName() {
		PollableChannel replyChannel = context.getBean("testReplyChannel", PollableChannel.class);
		MessageChannel inputChannel = context.getBean("replyChannelNameInput", MessageChannel.class);
		inputChannel.send(new GenericMessage<>("test"));
		Message<?> result = replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo("testReplyChannel");
	}

	@Test //INT-2316
	public void replyChannelExpression() {
		PollableChannel replyChannel = context.getBean("testReplyChannel", PollableChannel.class);
		MessageChannel inputChannel = context.getBean("replyChannelExpressionInput", MessageChannel.class);
		inputChannel.send(new GenericMessage<>("test"));
		Message<?> result = replyChannel.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("TEST");
		assertThat(result.getHeaders().getReplyChannel()).isEqualTo(replyChannel);
	}

	@Test
	public void errorChannel() {
		PollableChannel errorChannel = context.getBean("testErrorChannel", PollableChannel.class);
		MessageChannel inputChannel = context.getBean("errorChannelInput", MessageChannel.class);
		inputChannel.send(new GenericMessage<>("test"));
		Message<?> errorMessage = errorChannel.receive(10000);
		assertThat(errorMessage).isNotNull();
		Object errorPayload = errorMessage.getPayload();
		assertThat(errorPayload.getClass()).isEqualTo(MessageTransformationException.class);
		Message<?> failedMessage = ((MessageTransformationException) errorPayload).getFailedMessage();
		assertThat(failedMessage.getPayload()).isEqualTo("test");
		assertThat(failedMessage.getHeaders().getErrorChannel()).isEqualTo(errorChannel);
	}

	@Test
	public void correlationIdValue() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdValueInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getCorrelationId()).isEqualTo("ABC");
	}

	@Test
	public void correlationIdValueWithType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdValueWithTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		Object correlationId = new IntegrationMessageHeaderAccessor(result).getCorrelationId();
		assertThat(correlationId.getClass()).isEqualTo(Long.class);
		assertThat(correlationId).isEqualTo(123L);
	}

	@Test
	public void correlationIdRef() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("correlationIdRefInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getCorrelationId()).isEqualTo(123);
	}

	@Test
	public void expirationDateValue() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expirationDateValueInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getExpirationDate()).isEqualTo(1111L);
	}

	@Test
	public void expirationDateRef() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expirationDateRefInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getExpirationDate()).isEqualTo(9999);
	}

	@Test
	public void priority() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("priorityInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getPriority()).isEqualTo(42);
	}

	@Test
	public void priorityExpression() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("priorityExpressionInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel,
				new GenericMessage<>(Collections.singletonMap("priority", "-10")));
		assertThat(result).isNotNull();
		assertThat(new IntegrationMessageHeaderAccessor(result).getPriority()).isEqualTo(-10);
	}

	@Test
	public void expressionUsingPayload() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("payloadExpressionInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>(new TestBean("foo")));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader")).isEqualTo("foobar");
	}

	@Test
	public void expressionUsingHeader() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("headerExpressionInput", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("test").setHeader("testHeader1", "foo").build();
		Message<?> result = template.sendAndReceive(channel, message);
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader2")).isEqualTo("foobar");
	}

	@Test
	public void expressionWithDateType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expressionWithDateTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		Object headerValue = result.getHeaders().get("currentDate");
		assertThat(headerValue.getClass()).isEqualTo(Date.class);
		Date date = (Date) headerValue;
		assertThat(new Date().getTime() - date.getTime() < 1000).isTrue();
	}

	@Test
	public void expressionWithLongType() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("expressionWithLongTypeInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("number").getClass()).isEqualTo(Long.class);
		assertThat(result.getHeaders().get("number")).isEqualTo(12345L);
	}

	@Test
	public void refWithMethod() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("refWithMethod", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader").getClass()).isEqualTo(String.class);
		assertThat(result.getHeaders().get("testHeader")).isEqualTo("testBeanForMethodInvoker");
	}

	@Test
	public void ref() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("ref", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader").getClass()).isEqualTo(TestBean.class);
		TestBean testBeanForRef = context.getBean("testBean1", TestBean.class);
		assertThat(result.getHeaders().get("testHeader")).isSameAs(testBeanForRef);
	}

	@Test
	public void innerBean() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("innerBean", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader").getClass()).isEqualTo(TestBean.class);
		TestBean testBeanForInnerBean = new TestBean("testBeanForInnerBean");
		assertThat(result.getHeaders().get("testHeader")).isEqualTo(testBeanForInnerBean);
	}

	@Test
	public void innerBeanWithMethod() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("innerBeanWithMethod", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		assertThat(result.getHeaders().get("testHeader").getClass()).isEqualTo(String.class);
		assertThat(result.getHeaders().get("testHeader")).isEqualTo("testBeanForInnerBeanWithMethod");
	}

	@Test(expected = BeanDefinitionParsingException.class)
	public void testFailConfigUnexpectedSubElement() {
		new ClassPathXmlApplicationContext("HeaderEnricherWithUnexpectedSubElementForHeader-fail-context.xml",
				this.getClass()).close();
	}

	@Test
	public void testRoutingSlip() {
		MessagingTemplate template = new MessagingTemplate();
		MessageChannel channel = context.getBean("routingSlipInput", MessageChannel.class);
		Message<?> result = template.sendAndReceive(channel, new GenericMessage<>("test"));
		assertThat(result).isNotNull();
		Object routingSlip = new IntegrationMessageHeaderAccessor(result)
				.getHeader(IntegrationMessageHeaderAccessor.ROUTING_SLIP);
		assertThat(routingSlip).isNotNull();
		assertThat(routingSlip).isInstanceOf(Map.class);
		@SuppressWarnings("unchecked")
		List<Object> routingSlipPath = (List<Object>) ((Map<?, ?>) routingSlip).keySet().iterator().next();

		assertThat(routingSlipPath.get(0)).isInstanceOf(ExpressionEvaluatingRoutingSlipRouteStrategy.class);
		assertThat(routingSlipPath.get(1)).isEqualTo("fooChannel");
		assertThat(routingSlipPath.get(2)).isInstanceOf(ExpressionEvaluatingRoutingSlipRouteStrategy.class);
		assertThat(routingSlipPath.get(3)).isEqualTo("bazRoutingSlip");
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
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			TestBean testBean = (TestBean) o;

			return Objects.equals(name, testBean.name);

		}

		@Override
		public int hashCode() {
			return name != null ? name.hashCode() : 0;
		}

	}

}
