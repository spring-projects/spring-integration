/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.amqp.outbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.CorrelationData.Confirm;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.amqp.support.NackedAmqpMessageException;
import org.springframework.integration.amqp.support.ReturnedAmqpMessageException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mapping.support.JsonHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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
	private MessageChannel multiSendChannel;

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

	@Autowired
	@Qualifier("withReturns.handler")
	private AmqpOutboundEndpoint withReturns;

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
		assertThat(ack).isNotNull();
		assertThat(ack.getPayload()).isEqualTo("foo");
		assertThat(ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM)).isEqualTo(Boolean.TRUE);

		org.springframework.amqp.core.Message received = this.amqpTemplateConfirms.receive(this.queue.getName());
		assertThat(new String(received.getBody(), "UTF-8")).isEqualTo("\"hello\"");
		assertThat(received.getMessageProperties().getContentType()).isEqualTo("application/json");
		assertThat(received.getMessageProperties().getHeaders()
				.get(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, ""))).isEqualTo("java.lang.String");

		// test whole message is correlation
		message = MessageBuilder.withPayload("hello")
				.build();
		this.pcMessageCorrelationRequestChannel.send(message);
		ack = ackChannel.receive(10000);
		assertThat(ack).isNotNull();
		assertThat(ack.getPayload()).isSameAs(message.getPayload());
		assertThat(ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM)).isEqualTo(Boolean.TRUE);

		while (this.amqpTemplateConfirms.receive(this.queue.getName()) != null) {
			// drain
		}
	}

	@Test
	public void adapterWithPublisherConfirms() {
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("amqp_confirmCorrelationData", "foo")
				.build();
		this.pcRequestChannelForAdapter.send(message);
		Message<?> ack = this.ackChannel.receive(10000);
		assertThat(ack).isNotNull();
		assertThat(ack.getPayload()).isEqualTo("foo");
		assertThat(ack.getHeaders().get(AmqpHeaders.PUBLISH_CONFIRM)).isEqualTo(Boolean.TRUE);
	}

	@Test
	public void multiSend() throws Exception {
		RabbitTemplate template = new RabbitTemplate(this.connectionFactory);
		template.setDefaultReceiveQueue(this.queue.getName());
		while (template.receive() != null) {
			// drain
		}
		Collection<Message<?>> messages = new ArrayList<>();
		messages.add(new GenericMessage<>("foo"));
		messages.add(new GenericMessage<>("bar"));
		Message<?> message = MessageBuilder.withPayload(messages)
				.build();
		this.multiSendChannel.send(message);
		org.springframework.amqp.core.Message m = receive(template);
		assertThat(m).isNotNull();
		assertThat(new String(m.getBody(), "UTF-8")).isEqualTo("foo");
		m = receive(template);
		assertThat(m).isNotNull();
		assertThat(new String(m.getBody(), "UTF-8")).isEqualTo("bar");
	}

	@Test
	public void syncConfirmTimeout() {
		Message<?> message = new GenericMessage<>("foo");
		RabbitTemplate template = spy(RabbitTemplate.class);
		willDoNothing().given(template).send(isNull(), isNull(), any(), any());
		List<CorrelationData> correlationList = new ArrayList<>();
		willReturn(correlationList).given(template).getUnconfirmed(100L);
		ArgumentCaptor<CorrelationData> correlationCaptor = ArgumentCaptor.forClass(CorrelationData.class);
		AmqpOutboundEndpoint endpoint = new AmqpOutboundEndpoint(template);
		PollableChannel nacks = new QueueChannel();
		endpoint.setConfirmNackChannel(nacks);
		endpoint.setConfirmCorrelationExpressionString("headers.id");
		endpoint.setBeanFactory(mock(BeanFactory.class));
		ThreadPoolTaskScheduler sched = new ThreadPoolTaskScheduler();
		endpoint.setTaskScheduler(sched);
		endpoint.setConfirmTimeout(100);
		sched.afterPropertiesSet();
		endpoint.setTaskScheduler(sched);
		endpoint.afterPropertiesSet();
		endpoint.start();
		endpoint.handleMessage(message);
		verify(template).send(isNull(), isNull(), any(), correlationCaptor.capture());
		CorrelationData correlation = correlationCaptor.getValue();
		correlationList.add(correlation);
		assertThat(TestUtils.getPropertyValue(correlation, "message", Message.class)).isSameAs(message);
		Message<?> nack = nacks.receive(10_000);
		assertThat(nack).isNotNull();
		assertThat(nack.getPayload()).isInstanceOf(NackedAmqpMessageException.class);
		assertThat(((NackedAmqpMessageException) nack.getPayload()).getFailedMessage()).isSameAs(message);
		assertThat(((NackedAmqpMessageException) nack.getPayload()).getCorrelationData())
			.isSameAs(message.getHeaders().getId());
		assertThat(((NackedAmqpMessageException) nack.getPayload()).getNackReason()).isEqualTo("Confirm timed out");
		endpoint.stop();
		sched.destroy();
	}

	@Test
	public void adapterWithReturns() throws Exception {
		this.withReturns.setErrorMessageStrategy(null);
		CorrelationData corrData = new CorrelationData("adapterWithReturns");
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader("corrData", corrData)
				.build();
		this.returnRequestChannel.send(message);
		Message<?> returned = returnChannel.receive(10000);
		assertThat(returned).isNotNull();
		assertThat(returned.getPayload()).isEqualTo(message.getPayload());
		Confirm confirm = corrData.getFuture().get(10, TimeUnit.SECONDS);
		assertThat(confirm).isNotNull();
		assertThat(confirm.isAck()).isTrue();
		assertThat(corrData.getReturned()).isNotNull();
	}

	@Test
	public void adapterWithReturnsAndErrorMessageStrategy() {
		Message<?> message = MessageBuilder.withPayload("hello").build();
		this.returnRequestChannel.send(message);
		Message<?> returned = returnChannel.receive(10000);
		assertThat(returned).isNotNull();
		assertThat(returned).isInstanceOf(ErrorMessage.class);
		assertThat(returned.getPayload()).isInstanceOf(ReturnedAmqpMessageException.class);
		ReturnedAmqpMessageException payload = (ReturnedAmqpMessageException) returned.getPayload();
		assertThat(payload.getFailedMessage().getPayload()).isEqualTo(message.getPayload());
	}

	@Test
	public void adapterWithContentType() throws Exception {
		RabbitTemplate template = new RabbitTemplate(this.connectionFactory);
		template.setDefaultReceiveQueue(this.queue.getName());
		while (template.receive() != null) {
			// drain
		}
		Message<?> message = MessageBuilder.withPayload("hello")
				.setHeader(AmqpHeaders.CONTENT_TYPE, "application/json")
				.build();
		this.ctRequestChannel.send(message);
		org.springframework.amqp.core.Message m = receive(template);
		assertThat(m).isNotNull();
		assertThat(new String(m.getBody(), "UTF-8")).isEqualTo("\"hello\"");
		assertThat(m.getMessageProperties().getContentType()).isEqualTo("application/json");
		assertThat(m.getMessageProperties().getHeaders().get(JsonHeaders.TYPE_ID.replaceFirst(JsonHeaders.PREFIX, "")))
				.isEqualTo("java.lang.String");
		message = MessageBuilder.withPayload("hello")
				.build();
		this.ctRequestChannel.send(message);
		m = receive(template);
		assertThat(m).isNotNull();
		assertThat(new String(m.getBody(), "UTF-8")).isEqualTo("hello");
		assertThat(m.getMessageProperties().getContentType()).isEqualTo("text/plain");
		while (template.receive() != null) {
			// drain
		}
	}

	private org.springframework.amqp.core.Message receive(RabbitTemplate template) throws Exception {
		int n = 0;
		org.springframework.amqp.core.Message message = template.receive();
		while (message == null && n++ < 100) {
			Thread.sleep(100);
			message = template.receive();
		}
		assertThat(message).isNotNull();
		return message;
	}

}
