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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
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
public class XPathHeaderEnricherParserTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private ApplicationContext context;

	private final Message<?> message = MessageBuilder.withPayload("<person name='John Doe' age='42' married='true'/>").build();


	@Test
	public void stringResultByDefault() {
		Message<?> result = this.getResultMessage();
		assertEquals("John Doe", result.getHeaders().get("name"));
	}

	@Test
	public void numberResult() {
		Message<?> result = this.getResultMessage();
		assertEquals(42, result.getHeaders().get("age"));
	}

	@Test
	public void booleanResult() {
		Message<?> result = this.getResultMessage();
		assertEquals(Boolean.TRUE, result.getHeaders().get("married"));
	}

	@Test
	public void nodeResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-test");
		assertTrue(header instanceof Node);
		Node node = (Node) header;
		assertEquals("42", node.getTextContent());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void nodeListResult() {
		Message<?> result = this.getResultMessage();
		Object header = result.getHeaders().get("node-list-test");
		assertTrue(List.class.isAssignableFrom(header.getClass()));
		List<Node> nodeList = (List<Node>) header;
		assertEquals(3, nodeList.size());
	}

	@Test
	public void expressionRef() {
		Message<?> result = this.getResultMessage();
		assertEquals(new Double(84), result.getHeaders().get("ref-test"));
	}

	@Test
	public void defaultOverwrite() {
		assertEquals(false, this.getEnricherProperty("defaultHeaderEnricher", "defaultOverwrite"));
	}

	@Test
	public void defaultShouldSkipNulls() {
		assertEquals(true, this.getEnricherProperty("defaultHeaderEnricher", "shouldSkipNulls"));
	}

	@Test
	public void customOverwrite() {
		assertEquals(true, this.getEnricherProperty("customHeaderEnricher", "defaultOverwrite"));
	}

	@Test
	public void customShouldSkipNulls() {
		assertEquals(false, this.getEnricherProperty("customHeaderEnricher", "shouldSkipNulls"));
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
		assertEquals("John Doe", reply.getHeaders().get("foo"));
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
		assertEquals("bar", reply.getHeaders().get("foo"));
	}


	private Message<?> getResultMessage() {
		this.input.send(message);
		return output.receive(0);
	}

	private boolean getEnricherProperty(String beanName, String propertyName) {
		Object endpoint = this.context.getBean(beanName);
		Object handler = new DirectFieldAccessor(endpoint).getPropertyValue("handler");
		Object enricher = new DirectFieldAccessor(handler).getPropertyValue("transformer");
		return ((Boolean) new DirectFieldAccessor(enricher).getPropertyValue(propertyName)).booleanValue();
	}

}
