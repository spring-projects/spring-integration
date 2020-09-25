/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.jms.dsl;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.jms.AbstractJmsChannel;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.lang.Nullable;

/**
 * A {@link MessageChannelSpec} for an {@link AbstractJmsChannel}.
 *
 * @param <S> the target {@link JmsPollableMessageChannelSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public class JmsPollableMessageChannelSpec<S extends JmsPollableMessageChannelSpec<S, T>, T extends AbstractJmsChannel>
		extends MessageChannelSpec<S, T> {

	protected final JmsChannelFactoryBean jmsChannelFactoryBean; // NOSONAR - final

	protected JmsPollableMessageChannelSpec(ConnectionFactory connectionFactory) {
		this(new JmsChannelFactoryBean(false), connectionFactory);
	}

	protected JmsPollableMessageChannelSpec(JmsChannelFactoryBean jmsChannelFactoryBean,
			ConnectionFactory connectionFactory) {

		this.jmsChannelFactoryBean = jmsChannelFactoryBean;
		this.jmsChannelFactoryBean.setConnectionFactory(connectionFactory);
		this.jmsChannelFactoryBean.setSingleton(false);
		this.jmsChannelFactoryBean.setBeanFactory(new DefaultListableBeanFactory());
	}

	@Override
	protected S id(@Nullable String id) {
		if (id != null) {
			this.jmsChannelFactoryBean.setBeanName(id);
		}
		return super.id(id);
	}

	/**
	 * Configure the destination name that backs this channel.
	 * @param destination the destination.
	 * @return the current {@link MessageChannelSpec}.
	 */
	public S destination(String destination) {
		this.jmsChannelFactoryBean.setDestinationName(destination);
		return _this();
	}

	/**
	 * @param destinationResolver the destinationResolver.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setDestinationResolver(DestinationResolver)
	 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setDestinationResolver(DestinationResolver)
	 */
	public S destinationResolver(DestinationResolver destinationResolver) {
		this.jmsChannelFactoryBean.setDestinationResolver(destinationResolver);
		return _this();
	}

	/**
	 * Configure the destination that backs this channel.
	 * @param destination the destination.
	 * @return the current {@link MessageChannelSpec}.
	 */
	public S destination(Destination destination) {
		this.jmsChannelFactoryBean.setDestination(destination);
		return _this();
	}

	/**
	 * Configure a message selector in the
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer} (when message driven)
	 * or the {@link org.springframework.jms.core.JmsTemplate} (when polled).
	 * @param messageSelector the messageSelector.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setMessageSelector(String)
	 * @see org.springframework.jms.core.JmsTemplate#receiveSelectedAndConvert(String)
	 */
	public S messageSelector(String messageSelector) {
		this.jmsChannelFactoryBean.setMessageSelector(messageSelector);
		return _this();
	}

	/**
	 * Configure the {@link MessageConverter} used for both sending and
	 * receiving.
	 * @param messageConverter the messageConverter.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setMessageConverter(MessageConverter)
	 */
	public S jmsMessageConverter(MessageConverter messageConverter) {
		this.jmsChannelFactoryBean.setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param deliveryPersistent the deliveryPersistent.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setDeliveryPersistent(boolean)
	 */
	public S deliveryPersistent(boolean deliveryPersistent) {
		this.jmsChannelFactoryBean.setDeliveryPersistent(deliveryPersistent);
		return _this();
	}

	/**
	 * @param explicitQosEnabled the explicitQosEnabled.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setExplicitQosEnabled(boolean)
	 */
	public S explicitQosEnabled(boolean explicitQosEnabled) {
		this.jmsChannelFactoryBean.setExplicitQosEnabled(explicitQosEnabled);
		return _this();
	}

	/**
	 * @param messageIdEnabled the messageIdEnabled.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setMessageIdEnabled(boolean)
	 */
	public S messageIdEnabled(boolean messageIdEnabled) {
		this.jmsChannelFactoryBean.setMessageIdEnabled(messageIdEnabled);
		return _this();
	}

	/**
	 * @param messageTimestampEnabled the messageTimestampEnabled.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setMessageTimestampEnabled(boolean)
	 */
	public S messageTimestampEnabled(boolean messageTimestampEnabled) {
		this.jmsChannelFactoryBean.setMessageTimestampEnabled(messageTimestampEnabled);
		return _this();
	}

	/**
	 * Default priority. May be overridden at run time with a message
	 * priority header.
	 * @param priority the priority.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setPriority(int)
	 */
	public S priority(int priority) {
		this.jmsChannelFactoryBean.setPriority(priority);
		return _this();
	}

	/**
	 * @param timeToLive the timeToLive.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setTimeToLive(long)
	 */
	public S timeToLive(long timeToLive) {
		this.jmsChannelFactoryBean.setTimeToLive(timeToLive);
		return _this();
	}

	/**
	 * @param receiveTimeout the receiveTimeout.
	 * @return the current {@link MessageChannelSpec}.
	 * @see org.springframework.jms.core.JmsTemplate#setReceiveTimeout(long)
	 * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setReceiveTimeout(long)
	 */
	public S receiveTimeout(long receiveTimeout) {
		this.jmsChannelFactoryBean.setReceiveTimeout(receiveTimeout);
		return _this();
	}

	/**
	 * @param sessionAcknowledgeMode the acknowledgement mode constant
	 * @return the current {@link MessageChannelSpec}.
	 * @see javax.jms.Session#AUTO_ACKNOWLEDGE etc.
	 */
	public S sessionAcknowledgeMode(int sessionAcknowledgeMode) {
		this.jmsChannelFactoryBean.setSessionAcknowledgeMode(sessionAcknowledgeMode);
		return _this();
	}

	/**
	 * Configure transactional sessions for both the
	 * {@link org.springframework.jms.core.JmsTemplate} (sends and polled receives) and
	 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}
	 * (message-driven receives).
	 * @param sessionTransacted the sessionTransacted.
	 * @return the current {@link MessageChannelSpec}.
	 */
	public S sessionTransacted(boolean sessionTransacted) {
		this.jmsChannelFactoryBean.setSessionTransacted(sessionTransacted);
		return _this();
	}

	@Override
	@SuppressWarnings("unchecked")
	protected T doGet() {
		try {
			this.channel = (T) this.jmsChannelFactoryBean.getObject();
		}
		catch (Exception e) {
			throw new BeanCreationException("Cannot create the JMS MessageChannel", e);
		}
		return super.doGet();
	}

}
