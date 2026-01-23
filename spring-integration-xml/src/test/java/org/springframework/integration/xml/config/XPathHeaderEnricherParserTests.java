/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.xml.config;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.transformer.support.HeaderValueMessageProcessor;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class XPathHeaderEnricherParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private ApplicationContext context;

	private final Message<?> message =
			MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>").build();

	@Test
	public void testParse() {
		EventDrivenConsumer consumer = (EventDrivenConsumer) context.getBean("parseOnly");
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "handler.order")).isEqualTo(2);
		assertThat(TestUtils.<Long>getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<Integer>getPropertyValue(consumer, "phase")).isEqualTo(-1);
		assertThat(TestUtils.<Boolean>getPropertyValue(consumer, "autoStartup")).isFalse();
		SmartLifecycleRoleController roleController = context.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.<MultiValueMap<?, ?>>getPropertyValue(
				roleController, "lifecycles").get("foo");
		assertThat(list).containsExactly(consumer);
	}

	@Test
	public void stringResultByDefault() {
		Message<?> result = this.getResultMessage();
		assertThat(result.getHeaders().get("name")).isEqualTo("John Doe");
	}

	@Test
	public void numberResult() {
		Message<?> result = this.getResultMessage();
		assertThat(result.getHeaders().get("age")).isEqualTo(42);
	}

	@Test
	public void booleanResult() {
		Message<?> result = this.getResultMessage();
		assertThat(result.getHeaders().get("married")).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void nodeResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-test");
		assertThat(header instanceof Node).isTrue();
		Node node = (Node) header;
		assertThat(node.getTextContent()).isEqualTo("42");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-list-test");
		assertThat(header).isInstanceOf(List.class);
		List<Node> nodeList = (List<Node>) header;
		assertThat(nodeList).isNotNull();
		assertThat(nodeList.size()).isEqualTo(3);
	}

	@Test
	public void expressionRef() {
		Message<?> result = getResultMessage();
		assertThat(result.getHeaders().get("ref-test")).isEqualTo(84d);
	}

	@Test
	public void testDefaultHeaderEnricher() {
		assertThat(getEnricherProperty("defaultHeaderEnricher", "defaultOverwrite")).isFalse();
		assertThat(getEnricherProperty("defaultHeaderEnricher", "shouldSkipNulls")).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testCustomHeaderEnricher() {
		assertThat(getEnricherProperty("customHeaderEnricher", "defaultOverwrite")).isTrue();
		assertThat(getEnricherProperty("customHeaderEnricher", "shouldSkipNulls")).isFalse();
		Map<String, ? extends HeaderValueMessageProcessor<?>> headersToAdd =
				TestUtils.getPropertyValue(this.context.getBean("customHeaderEnricher"),
						"handler.transformer.headersToAdd");
		HeaderValueMessageProcessor<?> headerValueMessageProcessor = headersToAdd.get("foo");
		assertThat(headerValueMessageProcessor)
				.isInstanceOf(XPathExpressionEvaluatingHeaderValueMessageProcessor.class);
		assertThat(TestUtils.<Object>getPropertyValue(headerValueMessageProcessor, "converter"))
				.isSameAs(this.context.getBean("xmlPayloadConverter"));
	}

	@Test
	public void childOverridesDefaultOverwrite() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> request = MessageBuilder.fromMessage(this.message)
				.setHeader("foo", "bar")
				.setReplyChannel(replyChannel)
				.build();
		this.context.getBean("defaultInput", MessageChannel.class).send(request);
		Message<?> reply = replyChannel.receive();
		assertThat(reply).isNotNull();
		assertThat(reply.getHeaders().get("foo")).isEqualTo("John Doe");
	}

	@Test
	public void childOverridesCustomOverwrite() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> request = MessageBuilder.fromMessage(this.message)
				.setHeader("foo", "bar")
				.setReplyChannel(replyChannel)
				.build();
		this.context.getBean("customInput", MessageChannel.class).send(request);
		Message<?> reply = replyChannel.receive();
		assertThat(reply).isNotNull();
		assertThat(reply.getHeaders().get("foo")).isEqualTo("bar");
	}

	private Message<?> getResultMessage() {
		this.input.send(message);
		return output.receive(0);
	}

	private boolean getEnricherProperty(String beanName, String propertyName) {
		Object endpoint = this.context.getBean(beanName);
		Object handler = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Object enricher = new DirectFieldAccessor(handler).getPropertyValue("transformer");
		return (boolean) new DirectFieldAccessor(enricher).getPropertyValue(propertyName);
	}

}
