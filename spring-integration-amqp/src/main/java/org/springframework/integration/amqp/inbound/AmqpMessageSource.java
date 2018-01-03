/*
 * Copyright 2018 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitUtils;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.RabbitExceptionTranslator;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.MessageSource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.AcknowledgmentCallback;
import org.springframework.integration.support.AcknowledgmentCallbackFactory;
import org.springframework.util.Assert;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;

/**
 * A pollable {@link MessageSource} for RabbitMQ.
 *
 * @author Gary Russell
 * @since 5.0.1
 *
 */
public class AmqpMessageSource extends AbstractMessageSource<Object> {

	private final String queue;

	private final ConnectionFactory connectionFactory;

	private final AmqpAckCallbackFactory ackCallbackFactory;

	private boolean transacted;

	private MessagePropertiesConverter propertiesConverter = new DefaultMessagePropertiesConverter();

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	public AmqpMessageSource(ConnectionFactory connectionFactory, String queue) {
		this(connectionFactory, new AmqpAckCallbackFactory(), queue);
	}

	public AmqpMessageSource(ConnectionFactory connectionFactory, AmqpAckCallbackFactory ackCallbackFactory,
			String queue) {
		Assert.notNull(connectionFactory, "'connectionFactory' cannot be null");
		Assert.notNull(ackCallbackFactory, "'ackCallbackFactory' cannot be null");
		Assert.notNull(queue, "'queue' cannot be null");
		this.connectionFactory = connectionFactory;
		this.ackCallbackFactory = ackCallbackFactory;
		this.queue = queue;
	}

	public boolean isTransacted() {
		return this.transacted;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	public MessagePropertiesConverter getPropertiesConverter() {
		return this.propertiesConverter;
	}

	public void setPropertiesConverter(MessagePropertiesConverter propertiesConverter) {
		this.propertiesConverter = propertiesConverter;
	}

	public AmqpHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	public String getComponentType() {
		return "amqp:message-source";
	}

	@Override
	protected AbstractIntegrationMessageBuilder<Object> doReceive() {
		Connection connection = this.connectionFactory.createConnection();
		Channel channel = connection.createChannel(this.transacted);
		try {
			GetResponse resp = channel.basicGet(this.queue, false);
			if (resp == null) {
				RabbitUtils.closeChannel(channel);
				RabbitUtils.closeConnection(connection);
				return null;
			}
			AcknowledgmentCallback callback = this.ackCallbackFactory
					.createCallback(new AmqpAckInfo(connection, channel, resp));
			MessageProperties messageProperties = this.propertiesConverter.toMessageProperties(resp.getProps(),
					resp.getEnvelope(), StandardCharsets.UTF_8.name());
			Map<String, Object> headers = this.headerMapper.toHeadersFromRequest(messageProperties);
			Object payload = this.messageConverter
					.fromMessage(new org.springframework.amqp.core.Message(resp.getBody(), messageProperties));
			return getMessageBuilderFactory().withPayload(payload)
					.copyHeaders(headers)
					.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, callback);
		}
		catch (IOException e) {
			RabbitUtils.closeChannel(channel);
			RabbitUtils.closeConnection(connection);
		}
		return null;
	}

	public static class AmqpAckCallbackFactory implements AcknowledgmentCallbackFactory<AmqpAckInfo> {

		@Override
		public AcknowledgmentCallback createCallback(AmqpAckInfo info) {
			return new AmqpAckCallback(info);
		}

	}

	public static class AmqpAckCallback implements AcknowledgmentCallback {

		private final AmqpAckInfo ackInfo;

		private volatile boolean acknowledged;

		public AmqpAckCallback(AmqpAckInfo ackInfo) {
			this.ackInfo = ackInfo;
		}

		@Override
		public void acknowledge(Status status) {
			Assert.notNull(status, "'status' cannot be null");
			try {
				long deliveryTag = this.ackInfo.getGetResponse().getEnvelope().getDeliveryTag();
				switch (status) {
				case ACCEPT:
					this.ackInfo.getChannel().basicAck(deliveryTag, false);
					break;
				case REJECT:
					this.ackInfo.getChannel().basicReject(deliveryTag, false);
					break;
				case REQUEUE:
					this.ackInfo.getChannel().basicReject(deliveryTag, true);
					break;
				default:
					break;
				}
			}
			catch (IOException e) {
				throw RabbitExceptionTranslator.convertRabbitAccessException(e);
			}
			finally {
				RabbitUtils.closeChannel(this.ackInfo.getChannel());
				RabbitUtils.closeConnection(this.ackInfo.getConnection());
				this.acknowledged = true;
			}
		}

		@Override
		public boolean isAcknowledged() {
			return this.acknowledged;
		}

	}

	/**
	 * Information for building an AmqpAckCallback.
	 */
	public static class AmqpAckInfo {

		private final Connection connection;

		private final Channel channel;

		private final GetResponse getResponse;

		public AmqpAckInfo(Connection connection, Channel channel, GetResponse getResponse) {
			this.connection = connection;
			this.channel = channel;
			this.getResponse = getResponse;
		}

		public Connection getConnection() {
			return this.connection;
		}

		public Channel getChannel() {
			return this.channel;
		}

		public GetResponse getGetResponse() {
			return this.getResponse;
		}

	}

}
