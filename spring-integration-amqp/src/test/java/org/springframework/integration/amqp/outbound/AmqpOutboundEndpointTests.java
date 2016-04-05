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

package org.springframework.integration.amqp.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.amqp.rule.BrokerRunning;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AmqpOutboundEndpointTests {

	@Rule
	public BrokerRunning brokerRunning = BrokerRunning.isRunning();

	@Autowired
	private MessageChannel pcRequestChannel;

	@Autowired
	private MessageChannel pcMessageCorrelationRequestChannel;

	@Autowired
	private RabbitTemplate amqpTemplateConfirms;

	@Autowired
	private Queue queue;

	@Autowired
	private PollableChannel ackChannel;

	@Autowired
	private MessageChannel pcRequestChannelForAdapter;

	@Autowired
	private MessageChannel returnRequestChannel;

	@Autowired
	private PollableChannel returnChannel;

	@Autowired
	private MessageChannel ctRequestChannel;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Test
	public void testGatewayPublisherConfirms() throws Exception {
		while (this.amqpTemplateConfirms.receive(this.queue.getName()) != null) {
			// drain
		}

		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("amqp_confirmCorrelationData", "foo")
				.setHeader(AmqpHeaders.CONTENT_TYPE, "application/json")
				.build();
		this.pcRequestChannel.send(message);
		Message<?> ack = this.ackChannel.receive(10000);
		assertNotNull(ack);
		assertEquals("foo", ack.getPayload());
		assertEquals(Boolean.TRUE, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));

		org.springframework.amqp.core.Message received = this.amqpTemplateConfirms.receive(this.queue.getName());
		assertEquals("\"hello\"", new String(received.getBody(), "UTF-8"));
		assertEquals("application/json", received.getMessageProperties().getContentType());
		assertEquals("java.lang.String", received.getMessageProperties().getHeaders()
				.get(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));

		// test whole message is correlation
		message = MessageBuilder.withPayload("hello")
				.build();
		this.pcMessageCorrelationRequestChannel.send(message);
		ack = ackChannel.receive(10000);
		assertNotNull(ack);
		assertSame(message.getPayload(), ack.getPayload());
		assertEquals(Boolean.TRUE, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));

		while (this.amqpTemplateConfirms.receive(this.queue.getName()) != null) {
			// drain
		}
	}

	@Test
	public void adapterWithPublisherConfirms() throws Exception {
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("amqp_confirmCorrelationData", "foo")
				.build();
		this.pcRequestChannelForAdapter.send(message);
		Message<?> ack = this.ackChannel.receive(10000);
		assertNotNull(ack);
		assertEquals("foo", ack.getPayload());
		assertEquals(Boolean.TRUE, ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM));
	}

	@Test
	public void adapterWithReturns() throws Exception {
		Message<?> message = MessageBuilder.withPayload("hello").build();
		this.returnRequestChannel.send(message);
		Message<?> returned = returnChannel.receive(10000);
		assertNotNull(returned);
		assertEquals(message.getPayload(), returned.getPayload());
	}

	@Test
	public void adapterWithContentType() throws Exception {
		RabbitTemplate template = new RabbitTemplate(this.connectionFactory);
		template.setQueue(this.queue.getName());
		while (template.receive() != null) {
			// drain
		}
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader(AmqpHeaders.CONTENT_TYPE, "application/json")
				.build();
		this.ctRequestChannel.send(message);
		org.springframework.amqp.core.Message m = template.receive();
		assertNotNull(m);
		assertEquals("\"hello\"", new String(m.getBody(), "UTF-8"));
		assertEquals("application/json", m.getMessageProperties().getContentType());
		assertEquals("java.lang.String",
				m.getMessageProperties().getHeaders().get(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")));
		message = MessageBuilder.withPayload("hello")
				.build();
		this.ctRequestChannel.send(message);
		m = template.receive();
		assertNotNull(m);
		assertEquals("hello", new String(m.getBody(), "UTF-8"));
		assertEquals("text/plain", m.getMessageProperties().getContentType());
		while (template.receive() != null) {
			// drain
		}
	}

}
