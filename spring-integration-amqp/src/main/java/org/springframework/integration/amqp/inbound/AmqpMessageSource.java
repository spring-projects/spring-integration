/*
 * Copyright 2018-2023 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.batch.SimpleBatchingStrategy;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitUtils;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.rabbit.support.RabbitExceptionTranslator;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.acks.AcknowledgmentCallbackFactory;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.util.Assert;

/**
 * A pollable {@link org.springframework.integration.core.MessageSource} for RabbitMQ.
 *
 * @author Gary Russell
 *
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

	private boolean rawMessageHeader;

	private BatchingStrategy batchingStrategy = new SimpleBatchingStrategy(0, 0, 0L);

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

	protected boolean isTransacted() {
		return this.transacted;
	}

	/**
	 * Set to true to use a transacted channel for the ack.
	 * @param transacted true for transacted.
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	protected MessagePropertiesConverter getPropertiesConverter() {
		return this.propertiesConverter;
	}

	/**
	 * Set a custom {@link MessagePropertiesConverter} to replace the default
	 * {@link DefaultMessagePropertiesConverter}.
	 * @param propertiesConverter the converter.
	 */
	public void setPropertiesConverter(MessagePropertiesConverter propertiesConverter) {
		this.propertiesConverter = propertiesConverter;
	}

	protected AmqpHeaderMapper getHeaderMapper() {
		return this.headerMapper;
	}

	/**
	 * Set a custom {@link AmqpHeaderMapper} to replace the default
	 * {@link DefaultAmqpHeaderMapper#inboundMapper()}.
	 * @param headerMapper the header mapper.
	 */
	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		this.headerMapper = headerMapper;
	}

	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * Set a custom {@link MessageConverter} to replace the default
	 * {@link SimpleMessageConverter}.
	 * @param messageConverter the converter.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	protected boolean isRawMessageHeader() {
		return this.rawMessageHeader;
	}

	/**
	 * Set to true to include the raw spring-amqp message as a header with key
	 * {@link AmqpMessageHeaderErrorMessageStrategy#AMQP_RAW_MESSAGE}, enabling callers to
	 * have access to the message to process errors. The raw message is also added to the
	 * common header {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param rawMessageHeader true to include the headers.
	 */
	public void setRawMessageHeader(boolean rawMessageHeader) {
		this.rawMessageHeader = rawMessageHeader;
	}

	protected BatchingStrategy getBatchingStrategy() {
		return this.batchingStrategy;
	}

	/**
	 * Set a batching strategy to use when de-batching messages.
	 * Default is {@link SimpleBatchingStrategy}.
	 * @param batchingStrategy the strategy.
	 * @since 5.2
	 */
	public void setBatchingStrategy(BatchingStrategy batchingStrategy) {
		Assert.notNull(batchingStrategy, "'batchingStrategy' cannot be null");
		this.batchingStrategy = batchingStrategy;
	}

	@Override
	public String getComponentType() {
		return "amqp:message-source";
	}

	@Override
	protected AbstractIntegrationMessageBuilder<Object> doReceive() {
		Connection connection = this.connectionFactory.createConnection(); // NOSONAR - RabbitUtils
		Channel channel = connection.createChannel(this.transacted);
		try {
			GetResponse resp = channel.basicGet(this.queue, false);
			if (resp == null) {
				RabbitUtils.closeChannel(channel);
				RabbitUtils.closeConnection(connection);
				return null;
			}
			AcknowledgmentCallback callback = this.ackCallbackFactory
					.createCallback(new AmqpAckInfo(connection, channel, this.transacted, resp));
			MessageProperties messageProperties = this.propertiesConverter.toMessageProperties(resp.getProps(),
					resp.getEnvelope(), StandardCharsets.UTF_8.name());
			messageProperties.setConsumerQueue(this.queue);
			Map<String, Object> headers = this.headerMapper.toHeadersFromRequest(messageProperties);
			org.springframework.amqp.core.Message amqpMessage = new org.springframework.amqp.core.Message(resp.getBody(), messageProperties);
			Object payload;
			if (this.batchingStrategy.canDebatch(messageProperties)) {
				List<Object> payloads = new ArrayList<>();
				this.batchingStrategy.deBatch(amqpMessage, fragment -> payloads
						.add(this.messageConverter.fromMessage(fragment)));
				payload = payloads;
			}
			else {
				payload = this.messageConverter.fromMessage(amqpMessage);
			}
			AbstractIntegrationMessageBuilder<Object> builder = getMessageBuilderFactory().withPayload(payload)
					.copyHeaders(headers)
					.setHeader(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK, callback);
			if (this.rawMessageHeader) {
				builder.setHeader(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE, amqpMessage);
				builder.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, amqpMessage);
			}
			return builder;
		}
		catch (IOException e) {
			RabbitUtils.closeChannel(channel);
			RabbitUtils.closeConnection(connection);
			throw RabbitExceptionTranslator.convertRabbitAccessException(e);
		}
	}

	public static class AmqpAckCallbackFactory implements AcknowledgmentCallbackFactory<AmqpAckInfo> {

		@Override
		public AcknowledgmentCallback createCallback(AmqpAckInfo info) {
			return new AmqpAckCallback(info);
		}

	}

	public static class AmqpAckCallback implements AcknowledgmentCallback {

		private static Log logger = LogFactory.getLog(AmqpAckCallback.class);

		private final AmqpAckInfo ackInfo;

		private boolean acknowledged;

		private boolean autoAckEnabled = true;

		public AmqpAckCallback(AmqpAckInfo ackInfo) {
			this.ackInfo = ackInfo;
		}

		protected AmqpAckInfo getAckInfo() {
			return this.ackInfo;
		}

		protected void setAcknowledged(boolean acknowledged) {
			this.acknowledged = acknowledged;
		}

		@Override
		public boolean isAcknowledged() {
			return this.acknowledged;
		}

		@Override
		public void noAutoAck() {
			this.autoAckEnabled = false;
		}

		@Override
		public boolean isAutoAck() {
			return this.autoAckEnabled;
		}

		@Override
		public void acknowledge(Status status) {
			Assert.notNull(status, "'status' cannot be null");
			if (logger.isTraceEnabled()) {
				logger.trace("acknowledge(" + status.name() + ") for " + this);
			}
			try {
				long deliveryTag = this.ackInfo.getGetResponse().getEnvelope().getDeliveryTag();
				switch (status) {
					case ACCEPT -> this.ackInfo.getChannel().basicAck(deliveryTag, false);
					case REJECT -> this.ackInfo.getChannel().basicReject(deliveryTag, false);
					case REQUEUE -> this.ackInfo.getChannel().basicReject(deliveryTag, true);
					default -> {
					}
				}
				if (this.ackInfo.isTransacted()) {
					this.ackInfo.getChannel().txCommit();
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
		public String toString() {
			return "AmqpAckCallback [ackInfo=" + this.ackInfo + ", acknowledged=" + this.acknowledged
					+ ", autoAckEnabled=" + this.autoAckEnabled + "]";
		}

	}

	/**
	 * Information for building an AmqpAckCallback.
	 */
	public static class AmqpAckInfo {

		private final Connection connection;

		private final Channel channel;

		private final boolean transacted;

		private final GetResponse getResponse;

		public AmqpAckInfo(Connection connection, Channel channel, boolean transacted, GetResponse getResponse) {
			this.connection = connection;
			this.channel = channel;
			this.transacted = transacted;
			this.getResponse = getResponse;
		}

		public Connection getConnection() {
			return this.connection;
		}

		public Channel getChannel() {
			return this.channel;
		}

		public boolean isTransacted() {
			return this.transacted;
		}

		public GetResponse getGetResponse() {
			return this.getResponse;
		}

		@Override
		public String toString() {
			return "AmqpAckInfo [connection=" + this.connection + ", channel=" + this.channel + ", transacted="
					+ this.transacted + ", getResponse=" + this.getResponse + "]";
		}

	}

}
