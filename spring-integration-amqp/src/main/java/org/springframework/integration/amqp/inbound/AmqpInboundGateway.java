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

package org.springframework.integration.amqp.inbound;

import java.util.Map;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
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
 * @author Artem Bilan
 * @since 2.1
 */
public class AmqpInboundGateway extends MessagingGatewaySupport {

	private final AbstractMessageListenerContainer messageListenerContainer;

	private final AmqpTemplate amqpTemplate;

	private final boolean amqpTemplateExplicitlySet;

	private volatile MessageConverter amqpMessageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private Address defaultReplyTo;

	public AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer) {
		this(listenerContainer, new RabbitTemplate(listenerContainer.getConnectionFactory()), false);
	}

	/**
	 * Construct {@link AmqpInboundGateway} based on the provided {@link AbstractMessageListenerContainer}
	 * to receive request messages and {@link AmqpTemplate} to send replies.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer} to receive AMQP messages.
	 * @param amqpTemplate the {@link AmqpTemplate} to send reply messages.
	 * @since 4.2
	 */
	public AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate) {
		this(listenerContainer, amqpTemplate, true);
	}

	private AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate,
	                           boolean amqpTemplateExplicitlySet) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.notNull(amqpTemplate, "'amqpTemplate' must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to an AMQP inbound Gateway " +
						"must not have a MessageListener configured since " +
						"the adapter needs to configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.amqpTemplate = amqpTemplate;
		this.amqpTemplateExplicitlySet = amqpTemplateExplicitlySet;
	}


	/**
	 * Specify the {@link MessageConverter} to convert request and reply to/from {@link Message}.
	 * If the {@link #amqpTemplate} is explicitly set, this {@link MessageConverter}
	 * isn't populated there. You must configure that external {@link #amqpTemplate}.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.amqpMessageConverter = messageConverter;
		if (!this.amqpTemplateExplicitlySet) {
			((RabbitTemplate) this.amqpTemplate).setMessageConverter(messageConverter);
		}
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * The {@code defaultReplyTo} address with the form
	 * <pre class="code">
	 * (exchange)/(routingKey)
	 * </pre>
	 * or
	 * <pre class="code">
	 * (queueName)
	 * </pre>
	 * if the request message doesn't have a {@code replyTo} property.
	 * The second form uses the default exchange ("") and the queue name as
	 * the routing key.
	 * @param defaultReplyTo the default {@code replyTo} address to use.
	 * @since 4.2
	 * @see Address
	 */
	public void setDefaultReplyTo(String defaultReplyTo) {
		this.defaultReplyTo = new Address(defaultReplyTo);
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
				Object payload = AmqpInboundGateway.this.amqpMessageConverter.fromMessage(message);
				Map<String, Object> headers =
						AmqpInboundGateway.this.headerMapper.toHeadersFromRequest(message.getMessageProperties());
				if (AmqpInboundGateway.this.messageListenerContainer.getAcknowledgeMode() == AcknowledgeMode.MANUAL) {
					headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
					headers.put(AmqpHeaders.CHANNEL, channel);
				}
				org.springframework.messaging.Message<?> request =
						getMessageBuilderFactory().withPayload(payload).copyHeaders(headers).build();
				final org.springframework.messaging.Message<?> reply = sendAndReceiveMessage(request);
				if (reply != null) {
					Address replyTo;
					String replyToProperty = message.getMessageProperties().getReplyTo();
					if (replyToProperty != null) {
						replyTo = new Address(replyToProperty);
					}
					else {
						replyTo = AmqpInboundGateway.this.defaultReplyTo;
					}

					MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {

						@Override
						public Message postProcessMessage(Message message) throws AmqpException {
							MessageProperties messageProperties = message.getMessageProperties();
							String contentEncoding = messageProperties.getContentEncoding();
							long contentLength = messageProperties.getContentLength();
							String contentType = messageProperties.getContentType();
							AmqpInboundGateway.this.headerMapper.fromHeadersToReply(reply.getHeaders(),
									messageProperties);
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

					};

					if (replyTo != null) {
						AmqpInboundGateway.this.amqpTemplate.convertAndSend(replyTo.getExchangeName(),
								replyTo.getRoutingKey(), reply.getPayload(), messagePostProcessor);
					}
					else {
						if (!AmqpInboundGateway.this.amqpTemplateExplicitlySet) {
							throw new IllegalStateException("There is no 'replyTo' message property " +
									"and the `defaultReplyTo` hasn't been configured.");
						}
						else {
							AmqpInboundGateway.this.amqpTemplate.convertAndSend(reply.getPayload(),
									messagePostProcessor);
						}
					}
				}
			}

		});
		this.messageListenerContainer.afterPropertiesSet();
		if (!this.amqpTemplateExplicitlySet) {
			((RabbitTemplate) this.amqpTemplate).afterPropertiesSet();
		}
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
