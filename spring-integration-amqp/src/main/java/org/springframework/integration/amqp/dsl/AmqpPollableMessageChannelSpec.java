/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.support.MessagePropertiesConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.amqp.channel.AbstractAmqpChannel;
import org.springframework.integration.amqp.config.AmqpChannelFactoryBean;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@link MessageChannelSpec} for a {@link AbstractAmqpChannel}s.
 *
 * @param <S> the target {@link AmqpPollableMessageChannelSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public class AmqpPollableMessageChannelSpec<S extends AmqpPollableMessageChannelSpec<S, T>, T extends AbstractAmqpChannel>
		extends MessageChannelSpec<S, T> {

	protected final AmqpChannelFactoryBean amqpChannelFactoryBean; // NOSONAR final

	protected AmqpPollableMessageChannelSpec(ConnectionFactory connectionFactory) {
		this(new AmqpChannelFactoryBean(false), connectionFactory);
	}

	AmqpPollableMessageChannelSpec(AmqpChannelFactoryBean amqpChannelFactoryBean, ConnectionFactory connectionFactory) {
		this.amqpChannelFactoryBean = amqpChannelFactoryBean;
		this.amqpChannelFactoryBean.setConnectionFactory(connectionFactory);
		this.amqpChannelFactoryBean.setSingleton(false);
		this.amqpChannelFactoryBean.setPubSub(false);

		/*
		We need this artificial BeanFactory to overcome AmqpChannelFactoryBean initialization.
		The real BeanFactory will be applied later for the target AbstractAmqpChannel instance.
		*/
		this.amqpChannelFactoryBean.setBeanFactory(new DefaultListableBeanFactory());
	}

	@Override
	protected S id(@Nullable String id) {
		this.amqpChannelFactoryBean.setBeanName(id);
		return super.id(id);
	}

	/**
	 * Also implicitly sets the {@link #id(String)} (if not explicitly set).
	 * @param queueName the queueName.
	 * @return the spec.
	 * @see AmqpChannelFactoryBean#setQueueName(String)
	 */
	public S queueName(String queueName) {
		if (getId() == null) {
			id(queueName + ".channel");
		}
		this.amqpChannelFactoryBean.setQueueName(queueName);
		return _this();
	}

	/**
	 * @param encoding the encoding.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.core.RabbitTemplate#setEncoding(String)
	 */
	public S encoding(String encoding) {
		this.amqpChannelFactoryBean.setEncoding(encoding);
		return _this();
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.core.RabbitTemplate#setMessageConverter(MessageConverter)
	 */
	public S amqpMessageConverter(MessageConverter messageConverter) {
		this.amqpChannelFactoryBean.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * Configure {@code channelTransacted} on both the
	 * {@link org.springframework.amqp.rabbit.core.RabbitTemplate} (for sends) and
	 * {@link org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer}
	 * (for receives) when using Spring Integration 4.0. When using Spring Integration
	 * 4.1, only the container is configured. See {@link #templateChannelTransacted(boolean)}.
	 * @param channelTransacted the channelTransacted.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.core.RabbitTemplate#setChannelTransacted(boolean)
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setChannelTransacted(boolean)
	 */
	public S channelTransacted(boolean channelTransacted) {
		this.amqpChannelFactoryBean.setChannelTransacted(channelTransacted);
		return _this();
	}

	/**
	 * Configure {@code channelTransacted} on the
	 * {@link org.springframework.amqp.rabbit.core.RabbitTemplate} used when sending
	 * messages to the channel. Only applies when Spring Integration 4.1 or greater is
	 * being used. Otherwise, see {@link #channelTransacted(boolean)}.
	 * @param channelTransacted the channelTransacted.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.core.RabbitTemplate#setChannelTransacted(boolean)
	 */
	public S templateChannelTransacted(boolean channelTransacted) {
		this.amqpChannelFactoryBean.setTemplateChannelTransacted(channelTransacted);
		return _this();
	}

	/**
	 * Configure {@code messagePropertiesConverter} on both the
	 * {@link org.springframework.amqp.rabbit.core.RabbitTemplate} (for sends) and
	 * {@link org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer}
	 * (for receives).
	 * @param messagePropertiesConverter the messagePropertiesConverter.
	 * @return the spec.
	 * @see org.springframework.amqp.rabbit.core.RabbitTemplate#setMessagePropertiesConverter
	 * @see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#setMessagePropertiesConverter
	 */
	public S messagePropertiesConverter(MessagePropertiesConverter messagePropertiesConverter) {
		this.amqpChannelFactoryBean.setMessagePropertiesConverter(messagePropertiesConverter);
		return _this();
	}

	/**
	 * Configure the delivery mode for messages that don't have an
	 * {@link org.springframework.amqp.support.AmqpHeaders#DELIVERY_MODE} header.
	 * Default is {@link MessageDeliveryMode#PERSISTENT}.
	 * @param mode the mode.
	 * @return the spec.
	 */
	public S defaultDeliveryMode(MessageDeliveryMode mode) {
		this.amqpChannelFactoryBean.setDefaultDeliveryMode(mode);
		return _this();
	}

	/**
	 * Configure whether normal spring-messaging to AMQP message mapping is enabled.
	 * Default false.
	 * @param extract true to enable mapping.
	 * @return the spec.
	 * @see #outboundHeaderMapper(AmqpHeaderMapper)
	 * @see #inboundHeaderMapper(AmqpHeaderMapper)
	 */
	public S extractPayload(boolean extract) {
		this.amqpChannelFactoryBean.setExtractPayload(extract);
		return _this();
	}

	/**
	 * Configure the outbound header mapper to use when {@link #extractPayload(boolean)}
	 * is true. Defaults to a {@code DefaultAmqpHeaderMapper}.
	 * @param mapper the mapper.
	 * @return the spec.
	 * @see #extractPayload(boolean)
	 */
	public S outboundHeaderMapper(AmqpHeaderMapper mapper) {
		this.amqpChannelFactoryBean.setOutboundHeaderMapper(mapper);
		return _this();
	}

	/**
	 * Configure the inbound header mapper to use when {@link #extractPayload(boolean)}
	 * is true. Defaults to a {@code DefaultAmqpHeaderMapper}.
	 * @param mapper the mapper.
	 * @return the spec.
	 * @see #extractPayload(boolean)
	 */
	public S inboundHeaderMapper(AmqpHeaderMapper mapper) {
		this.amqpChannelFactoryBean.setInboundHeaderMapper(mapper);
		return _this();
	}

	/**
	 * @param headersLast true to map headers last.
	 * @return the spec.
	 * @see AbstractAmqpChannel#setHeadersMappedLast(boolean)
	 */
	public S headersMappedLast(boolean headersLast) {
		this.target.setHeadersMappedLast(headersLast);
		return _this();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T doGet() {
		Assert.notNull(getId(), "The 'id' or 'queueName' must be specified");
		try {
			this.channel = (T) this.amqpChannelFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BeanCreationException("Cannot create the AMQP MessageChannel", e);
		}
		return super.doGet();
	}

}
