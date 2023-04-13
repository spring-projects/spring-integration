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

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link JmsDestinationPollingSource}.
 *
 * @param <S> the target {@link JmsInboundChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class JmsInboundChannelAdapterSpec<S extends JmsInboundChannelAdapterSpec<S>>
		extends MessageSourceSpec<S, JmsDestinationPollingSource> {

	protected final JmsTemplateSpec jmsTemplateSpec = new JmsTemplateSpec(); // NOSONAR final

	protected JmsInboundChannelAdapterSpec(JmsTemplate jmsTemplate) {
		this.target = new JmsDestinationPollingSource(jmsTemplate);
	}

	private JmsInboundChannelAdapterSpec(ConnectionFactory connectionFactory) {
		this.target =
				new JmsDestinationPollingSource(this.jmsTemplateSpec.connectionFactory(connectionFactory).getObject());
	}

	/**
	 * @param messageSelector the messageSelector.
	 * @return the spec.
	 * @see JmsDestinationPollingSource#setMessageSelector(String)
	 */
	public S messageSelector(String messageSelector) {
		this.target.setMessageSelector(messageSelector);
		return _this();
	}

	/**
	 * Configure a {@link JmsHeaderMapper} to map from JMS headers and properties to
	 * Spring Integration headers.
	 * @param headerMapper the headerMapper.
	 * @return the spec.
	 */
	public S headerMapper(JmsHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Configure the destination from which to receive messages.
	 * @param destination the destination.
	 * @return the spec.
	 */
	public S destination(Destination destination) {
		this.target.setDestination(destination);
		return _this();
	}

	/**
	 * Configure the name of destination from which to receive messages.
	 * @param destination the destination.
	 * @return the spec.
	 */
	public S destination(String destination) {
		this.target.setDestinationName(destination);
		return _this();
	}

	/**
	 * A {@link JmsTemplate}-based {@link JmsInboundChannelAdapterSpec} extension.
	 */
	public static class JmsInboundChannelSpecTemplateAware
			extends JmsInboundChannelAdapterSpec<JmsInboundChannelSpecTemplateAware>
			implements ComponentsRegistration {

		protected JmsInboundChannelSpecTemplateAware(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		/**
		 * Configure the channel adapter to use the template specification created by invoking the
		 * {@link Consumer} callback, passing in a {@link JmsTemplateSpec}.
		 * @param configurer the configurer.
		 * @return the spec.
		 */
		public JmsInboundChannelSpecTemplateAware configureJmsTemplate(Consumer<JmsTemplateSpec> configurer) {
			Assert.notNull(configurer, "'configurer' must not be null");
			configurer.accept(this.jmsTemplateSpec);
			return _this();
		}

		@Override
		public Map<Object, String> getComponentsToRegister() {
			return Collections.singletonMap(this.jmsTemplateSpec.getObject(), this.jmsTemplateSpec.getId());
		}

	}

}
