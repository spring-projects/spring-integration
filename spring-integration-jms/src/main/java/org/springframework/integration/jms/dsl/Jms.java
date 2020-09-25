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

package org.springframework.integration.jms.dsl;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import org.springframework.integration.jms.PollableJmsChannel;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.lang.Nullable;

/**
 * Factory class for JMS components.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 */
public final class Jms {

	/**
	 * The factory to produce a {@link JmsPollableMessageChannelSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPollableMessageChannelSpec} instance
	 */
	public static JmsPollableMessageChannelSpec<?, PollableJmsChannel> pollableChannel(ConnectionFactory connectionFactory) {
		return pollableChannel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsPollableMessageChannelSpec}.
	 * @param id                the bean name for the target {@code PollableChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPollableMessageChannelSpec} instance
	 */
	public static JmsPollableMessageChannelSpec<?, PollableJmsChannel> pollableChannel(@Nullable String id,
			ConnectionFactory connectionFactory) {
		JmsPollableMessageChannelSpec<?, PollableJmsChannel> spec = new JmsPollableMessageChannelSpec<>(connectionFactory);
		return spec.id(id);
	}

	/**
	 * The factory to produce a {@link JmsMessageChannelSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsMessageChannelSpec} instance
	 */
	public static JmsMessageChannelSpec<?, ?> channel(ConnectionFactory connectionFactory) {
		return channel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsMessageChannelSpec}.
	 * @param id                the bean name for the target {@code MessageChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsMessageChannelSpec} instance
	 */
	public static JmsMessageChannelSpec<?, ?> channel(@Nullable String id, ConnectionFactory connectionFactory) {
		return new JmsMessageChannelSpec<>(connectionFactory)
				.id(id);
	}

	/**
	 * The factory to produce a {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPublishSubscribeMessageChannelSpec} instance
	 */
	public static JmsPublishSubscribeMessageChannelSpec publishSubscribeChannel(ConnectionFactory connectionFactory) {
		return publishSubscribeChannel(null, connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsPublishSubscribeMessageChannelSpec}.
	 * @param id                the bean name for the target {@code MessageChannel} component
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsPublishSubscribeMessageChannelSpec} instance
	 */
	public static JmsPublishSubscribeMessageChannelSpec publishSubscribeChannel(@Nullable String id,
			ConnectionFactory connectionFactory) {

		return new JmsPublishSubscribeMessageChannelSpec(connectionFactory).id(id);
	}

	/**
	 * The factory to produce a {@link JmsOutboundChannelAdapterSpec}.
	 * @param jmsTemplate the JmsTemplate to build on
	 * @return the {@link JmsOutboundChannelAdapterSpec} instance
	 */
	public static JmsOutboundChannelAdapterSpec<?> outboundAdapter(JmsTemplate jmsTemplate) {
		return new JmsOutboundChannelAdapterSpec<>(jmsTemplate);
	}

	/**
	 * The factory to produce a {@link JmsOutboundChannelAdapterSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsOutboundChannelAdapterSpec} instance
	 */
	public static JmsOutboundChannelAdapterSpec.JmsOutboundChannelSpecTemplateAware outboundAdapter(
			ConnectionFactory connectionFactory) {

		return new JmsOutboundChannelAdapterSpec.JmsOutboundChannelSpecTemplateAware(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsInboundChannelAdapterSpec}.
	 * @param jmsTemplate the JmsTemplate to build on
	 * @return the {@link JmsInboundChannelAdapterSpec} instance
	 */
	public static JmsInboundChannelAdapterSpec<?> inboundAdapter(JmsTemplate jmsTemplate) {
		return new JmsInboundChannelAdapterSpec<>(jmsTemplate);
	}

	/**
	 * The factory to produce a {@link JmsInboundChannelAdapterSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsInboundChannelAdapterSpec} instance
	 */
	public static JmsInboundChannelAdapterSpec.JmsInboundChannelSpecTemplateAware inboundAdapter(
			ConnectionFactory connectionFactory) {

		return new JmsInboundChannelAdapterSpec.JmsInboundChannelSpecTemplateAware(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsOutboundGatewaySpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsOutboundGatewaySpec} instance
	 */
	public static JmsOutboundGatewaySpec outboundGateway(ConnectionFactory connectionFactory) {
		return new JmsOutboundGatewaySpec(connectionFactory);
	}

	/**
	 * The factory to produce a {@link JmsInboundGatewaySpec}.
	 * @param listenerContainer the JMS {@link AbstractMessageListenerContainer} to build on
	 * @return the {@link JmsInboundGatewaySpec} instance
	 */
	public static JmsInboundGatewaySpec<?> inboundGateway(AbstractMessageListenerContainer listenerContainer) {
		return new JmsInboundGatewaySpec<>(listenerContainer);
	}

	/**
	 * The factory to produce a {@link JmsInboundGatewaySpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsInboundGatewaySpec} instance
	 */
	public static JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<JmsDefaultListenerContainerSpec, DefaultMessageListenerContainer>
	inboundGateway(ConnectionFactory connectionFactory) {

		try {
			return new JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<>(
					new JmsDefaultListenerContainerSpec()
							.connectionFactory(connectionFactory));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsInboundGatewaySpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param containerClass    the {@link AbstractMessageListenerContainer} implementation class
	 *                          to instantiate listener container
	 * @param <C>               the {@link AbstractMessageListenerContainer} inheritor type
	 * @return the {@link JmsInboundGatewaySpec} instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <C extends AbstractMessageListenerContainer>
	JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec<?, C> inboundGateway(
			ConnectionFactory connectionFactory, Class<C> containerClass) {

		try {
			JmsListenerContainerSpec<?, C> spec =
					new JmsListenerContainerSpec<>(containerClass)
							.connectionFactory(connectionFactory);
			return new JmsInboundGatewaySpec.JmsInboundGatewayListenerContainerSpec(spec);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}.
	 * @param jmsListenerContainerSpec the {@link JmsListenerContainerSpec} to build on
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static JmsMessageDrivenChannelAdapterSpec<?> messageDrivenChannelAdapter(
			JmsListenerContainerSpec<?, ? extends AbstractMessageListenerContainer> jmsListenerContainerSpec) {

		return new JmsMessageDrivenChannelAdapterSpec<>(jmsListenerContainerSpec.get());
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer} to build on
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static JmsMessageDrivenChannelAdapterSpec<?> messageDrivenChannelAdapter(AbstractMessageListenerContainer listenerContainer) {
		return new JmsMessageDrivenChannelAdapterSpec<>(listenerContainer);
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	public static JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<JmsDefaultListenerContainerSpec, DefaultMessageListenerContainer>
	messageDrivenChannelAdapter(ConnectionFactory connectionFactory) {
		try {
			return new JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<>(
					new JmsDefaultListenerContainerSpec()
							.connectionFactory(connectionFactory));
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsMessageDrivenChannelAdapterSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param containerClass    the {@link AbstractMessageListenerContainer} implementation class
	 *                          to instantiate listener container
	 * @param <C>               the {@link AbstractMessageListenerContainer} inheritor type
	 * @return the {@link JmsMessageDrivenChannelAdapterSpec} instance
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <C extends AbstractMessageListenerContainer>
	JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec<?, C>
	messageDrivenChannelAdapter(ConnectionFactory connectionFactory, Class<C> containerClass) {
		try {
			JmsListenerContainerSpec<?, C> spec =
					new JmsListenerContainerSpec<>(containerClass)
							.connectionFactory(connectionFactory);
			return new JmsMessageDrivenChannelAdapterSpec.JmsMessageDrivenChannelAdapterListenerContainerSpec(spec);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsListenerContainerSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param destination       the {@link Destination} to listen to
	 * @return the {@link JmsListenerContainerSpec} instance
	 */
	public static JmsDefaultListenerContainerSpec container(ConnectionFactory connectionFactory,
			Destination destination) {
		try {
			return new JmsDefaultListenerContainerSpec()
					.connectionFactory(connectionFactory)
					.destination(destination);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * The factory to produce a {@link JmsListenerContainerSpec}.
	 * @param connectionFactory the JMS ConnectionFactory to build on
	 * @param destinationName   the destination name to listen to
	 * @return the {@link JmsListenerContainerSpec} instance
	 */
	public static JmsDefaultListenerContainerSpec container(ConnectionFactory connectionFactory,
			String destinationName) {
		try {
			return new JmsDefaultListenerContainerSpec()
					.connectionFactory(connectionFactory)
					.destination(destinationName);
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Jms() {
	}

}
