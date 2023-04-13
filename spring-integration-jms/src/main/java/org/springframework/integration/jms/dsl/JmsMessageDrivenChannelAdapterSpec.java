/*
 * Copyright 2016-2023 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import jakarta.jms.Destination;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageProducerSpec;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * A {@link MessageProducerSpec} for {@link JmsMessageDrivenEndpoint}s.
 *
 * @param <S> the target {@link JmsMessageDrivenChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class JmsMessageDrivenChannelAdapterSpec<S extends JmsMessageDrivenChannelAdapterSpec<S>>
		extends MessageProducerSpec<S, JmsMessageDrivenEndpoint> {

	protected JmsMessageDrivenChannelAdapterSpec(AbstractMessageListenerContainer listenerContainer) {
		super(new JmsMessageDrivenEndpoint(listenerContainer, new ChannelPublishingJmsMessageListener()));
		this.target.getListener().setExpectReply(false);
	}

	/**
	 * @param messageConverter the messageConverter.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setMessageConverter(MessageConverter)
	 */
	public S jmsMessageConverter(MessageConverter messageConverter) {
		this.target.getListener().setMessageConverter(messageConverter);
		return _this();
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setHeaderMapper(JmsHeaderMapper)
	 */
	public S headerMapper(JmsHeaderMapper headerMapper) {
		this.target.getListener().setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * @param extractRequestPayload the extractRequestPayload.
	 * @return the spec.
	 * @see ChannelPublishingJmsMessageListener#setExtractRequestPayload(boolean)
	 */
	public S extractPayload(boolean extractRequestPayload) {
		this.target.getListener().setExtractRequestPayload(extractRequestPayload);
		return _this();
	}

	/**
	 * Set to 'false' to prevent listener container shutdown when the endpoint is stopped.
	 * Then, if so configured, any cached consumer(s) in the container will remain.
	 * Otherwise, the shared connection and will be closed and the listener invokers shut
	 * down; this behavior is new starting with version 5.1. Default: true.
	 * @param shutdown false to not shutdown.
	 * @return the spec.
	 * @since 5.1
	 */
	public S shutdownContainerOnStop(boolean shutdown) {
		this.target.setShutdownContainerOnStop(shutdown);
		return _this();
	}

	/**
	 *
	 * @param <S> the target {@link JmsListenerContainerSpec} implementation type.
	 * @param <C> the target {@link AbstractMessageListenerContainer} implementation type.
	 */
	public static class
	JmsMessageDrivenChannelAdapterListenerContainerSpec<S extends JmsListenerContainerSpec<S, C>, C extends AbstractMessageListenerContainer>
			extends JmsMessageDrivenChannelAdapterSpec<JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C>>
			implements ComponentsRegistration {

		private final S spec;

		protected JmsMessageDrivenChannelAdapterListenerContainerSpec(S spec) {
			super(spec.getObject());
			this.spec = spec;
			this.spec.getObject().setAutoStartup(false);

		}

		/**
		 * @param destination the destination.
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(Destination)
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> destination(Destination destination) {
			this.spec.destination(destination);
			return _this();
		}

		/**
		 * Specify a destination name to use.
		 * @param destinationName the destinationName.
		 * @return the spec.
		 * @see JmsListenerContainerSpec#destination(String)
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> destination(String destinationName) {
			this.spec.destination(destinationName);
			return _this();
		}

		/**
		 * Configure a listener container by invoking the {@link Consumer} callback, with a
		 * {@link JmsListenerContainerSpec} argument.
		 * @param configurer the configurer.
		 * @return the spec.
		 */
		public JmsMessageDrivenChannelAdapterListenerContainerSpec<S, C> configureListenerContainer(
				Consumer<S> configurer) {

			Assert.notNull(configurer, "'configurer' must not be null");
			configurer.accept(this.spec);
			return _this();
		}

		@Override
		public Map<Object, String> getComponentsToRegister() {
			return Collections.singletonMap(this.spec.getObject(), this.spec.getId());
		}

	}

}
