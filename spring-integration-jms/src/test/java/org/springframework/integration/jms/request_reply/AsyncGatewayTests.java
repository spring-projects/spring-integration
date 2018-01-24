/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.jms.request_reply;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import javax.jms.Message;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.JmsTimeoutException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class AsyncGatewayTests {

	@Autowired
	private CachingConnectionFactory ccf;

	@Autowired
	private JmsOutboundGateway gateway1;

	@Autowired
	private JmsOutboundGateway gateway2;

	@Test
	public void testWithReply() throws Exception {
		QueueChannel replies = new QueueChannel();
		this.gateway1.setOutputChannel(replies);
		this.gateway1.start();
		this.gateway1.handleMessage(MessageBuilder.withPayload("foo")
				.setHeader(JmsHeaders.CORRELATION_ID, "baz")// make sure it's restored in case we're from an upstream gw
				.build());
		JmsTemplate template = new JmsTemplate(this.ccf);
		template.setReceiveTimeout(10000);
		final Message received = template.receive("asyncTest1");
		assertNotNull(received);
		template.send(received.getJMSReplyTo(), (MessageCreator) session -> {
			TextMessage textMessage = session.createTextMessage("bar");
			textMessage.setJMSCorrelationID(received.getJMSCorrelationID());
			return textMessage;
		});
		org.springframework.messaging.Message<?> reply = replies.receive(10000);
		assertNotNull(reply);
		assertEquals("bar", reply.getPayload());
		assertEquals("baz", reply.getHeaders().get(JmsHeaders.CORRELATION_ID));
		this.gateway1.stop();
	}

	@Test
	public void testWithTimeout() throws Exception {
		QueueChannel errors = new QueueChannel();
		this.gateway2.setOutputChannel(errors);
		this.gateway2.start();
		this.gateway2.handleMessage(MessageBuilder.withPayload("foo").setErrorChannel(errors).build());
		JmsTemplate template = new JmsTemplate(this.ccf);
		template.setReceiveTimeout(10000);
		final Message received = template.receive("asyncTest3");
		assertNotNull(received);
		org.springframework.messaging.Message<?> error = errors.receive(10000);
		assertNotNull(error);
		assertThat(error, instanceOf(ErrorMessage.class));
		assertThat(error.getPayload(), instanceOf(MessagingException.class));
		assertThat(((MessagingException) error.getPayload()).getCause(), instanceOf(JmsTimeoutException.class));
		assertEquals("foo", ((MessagingException) error.getPayload()).getFailedMessage().getPayload());
		this.gateway2.stop();
	}

	@Test
	@DirtiesContext
	public void testWithTimeoutNoReplyRequired() throws Exception {
		QueueChannel errors = new QueueChannel();
		this.gateway2.setOutputChannel(errors);
		this.gateway2.setRequiresReply(false);
		this.gateway2.start();
		this.gateway2.handleMessage(MessageBuilder.withPayload("foo").setErrorChannel(errors).build());
		JmsTemplate template = new JmsTemplate(this.ccf);
		template.setReceiveTimeout(10000);
		final Message received = template.receive("asyncTest3");
		assertNotNull(received);
		org.springframework.messaging.Message<?> error = errors.receive(1000);
		assertNull(error);
		this.gateway2.stop();
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public CachingConnectionFactory ccf() {
			CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(
					new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false"));
			cachingConnectionFactory.setCacheConsumers(false);
			return cachingConnectionFactory;
		}

		@Bean
		public JmsOutboundGateway gateway1() {
			JmsOutboundGateway gateway = new JmsOutboundGateway();
			gateway.setUseReplyContainer(true);
			gateway.setConnectionFactory(ccf());
			gateway.setRequestDestinationName("asyncTest1");
			gateway.setReplyDestinationName("asyncTest2");
			gateway.setRequiresReply(true);
			gateway.setReceiveTimeout(10000);
			gateway.setAsync(true);
			gateway.setCorrelationKey("JMSCorrelationID");
			return gateway;
		}

		@Bean
		public JmsOutboundGateway gateway2() {
			JmsOutboundGateway gateway = new JmsOutboundGateway();
			gateway.setUseReplyContainer(true);
			gateway.setConnectionFactory(ccf());
			gateway.setRequestDestinationName("asyncTest3");
			gateway.setReplyDestinationName("asyncTest4");
			gateway.setRequiresReply(true);
			gateway.setReceiveTimeout(10);
			gateway.setAsync(true);
			gateway.setCorrelationKey("JMSCorrelationID");
			return gateway;
		}

	}

}
