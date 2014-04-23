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

package org.springframework.integration.amqp.inbound;

import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.rabbitmq.client.Channel;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 * If a reply Message is received, it will be converted and sent back to
 * the AMQP 'replyTo'.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public class AmqpInboundGateway extends MessagingGatewaySupport {

	private final AbstractMessageListenerContainer messageListenerContainer;

	private volatile MessageConverter amqpMessageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = new DefaultAmqpHeaderMapper();

	private final RabbitTemplate amqpTemplate;


	public AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.isNull(listenerContainer.getMessageListener(), "The listenerContainer provided to an AMQP inbound Gateway " +
				"must not have a MessageListener configured since the adapter needs to configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.amqpTemplate = new RabbitTemplate(this.messageListenerContainer.getConnectionFactory());
	}


	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.amqpMessageConverter = messageConverter;
		this.amqpTemplate.setMessageConverter(messageConverter);
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-gateway";
	}

	@Override
	protected void onInit() throws Exception {
		this.messageListenerContainer.setMessageListener(new ChannelAwareMessageListener() {
			@Override
			public void onMessage(Message message, Channel channel) {
				Object payload = amqpMessageConverter.fromMessage(message);
				Map<String, Object> headers = headerMapper.toHeadersFromRequest(message.getMessageProperties());
				if (messageListenerContainer.getAcknowledgeMode() == AcknowledgeMode.MANUAL) {
					headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
					headers.put(AmqpHeaders.CHANNEL, channel);
				}
				org.springframework.messaging.Message<?> request =
						AmqpInboundGateway.this.getMessageBuilderFactory().withPayload(payload).copyHeaders(headers).build();
				final org.springframework.messaging.Message<?> reply = sendAndReceiveMessage(request);
				if (reply != null) {
					// TODO: fallback to a reply address property of this gateway
					Address replyTo = message.getMessageProperties().getReplyToAddress();
					Assert.notNull(replyTo, "The replyTo header must not be null on a " +
							"request Message being handled by the AMQP inbound gateway.");
					amqpTemplate.convertAndSend(replyTo.getExchangeName(), replyTo.getRoutingKey(), reply.getPayload(),
							new MessagePostProcessor() {
								@Override
								public Message postProcessMessage(Message message) throws AmqpException {
									MessageProperties messageProperties = message.getMessageProperties();
									String contentEncoding = messageProperties.getContentEncoding();
									long contentLength = messageProperties.getContentLength();
									String contentType = messageProperties.getContentType();
									headerMapper.fromHeadersToReply(reply.getHeaders(), messageProperties);
									// clear the replyTo from the original message since we are using it now
									messageProperties.setReplyTo(null);
									// reset the content-* properties as determined by the MessageConverter
									if (StringUtils.hasText(contentEncoding)) {
										messageProperties.setContentEncoding(contentEncoding);
									}
									messageProperties.setContentLength(contentLength);
									if (contentType != null) {
										messageProperties.setContentType(contentType);
									}
									return message;
								}
							});
				}
			}

		});
		this.messageListenerContainer.afterPropertiesSet();
		this.amqpTemplate.afterPropertiesSet();
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

}
