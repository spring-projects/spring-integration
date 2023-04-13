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
import java.util.function.Function;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;

import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.jms.JmsHeaderMapper;
import org.springframework.integration.jms.JmsSendingMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandlerSpec} for a {@link JmsSendingMessageHandler}.
 *
 * @param <S> the target {@link JmsOutboundChannelAdapterSpec} implementation type.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class JmsOutboundChannelAdapterSpec<S extends JmsOutboundChannelAdapterSpec<S>>
		extends MessageHandlerSpec<S, JmsSendingMessageHandler> {

	protected final JmsTemplateSpec jmsTemplateSpec = new JmsTemplateSpec(); // NOSONAR final

	protected JmsOutboundChannelAdapterSpec(JmsTemplate jmsTemplate) {
		this.target = new JmsSendingMessageHandler(jmsTemplate);
	}

	private JmsOutboundChannelAdapterSpec(ConnectionFactory connectionFactory) {
		this.target =
				new JmsSendingMessageHandler(this.jmsTemplateSpec.connectionFactory(connectionFactory).getObject());
	}

	/**
	 * @param extractPayload the extractPayload flag.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setExtractPayload(boolean)
	 */
	public S extractPayload(boolean extractPayload) {
		this.target.setExtractPayload(extractPayload);
		return _this();
	}

	/**
	 * @param headerMapper the headerMapper.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setHeaderMapper(JmsHeaderMapper)
	 */
	public S headerMapper(JmsHeaderMapper headerMapper) {
		this.target.setHeaderMapper(headerMapper);
		return _this();
	}

	/**
	 * Configure the destination to which this adapter will send messages.
	 * @param destination the destination.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setDestination(Destination)
	 */
	public S destination(Destination destination) {
		this.target.setDestination(destination);
		return _this();
	}

	/**
	 * Configure the name of the destination to which this adapter will send messages.
	 * @param destination the destination name.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setDestinationName(String)
	 */
	public S destination(String destination) {
		this.target.setDestinationName(destination);
		return _this();
	}

	/**
	 * Configure a SpEL expression that will evaluate, at run time, the destination to
	 * which a message will be sent.
	 * @param destination the destination name.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setDestinationExpression
	 */
	public S destinationExpression(String destination) {
		this.target.setDestinationExpression(PARSER.parseExpression(destination));
		return _this();
	}

	/**
	 * Configure a {@link Function} that will be invoked at run time to determine the destination to
	 * which a message will be sent. Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 * .<Foo>destination(m -> m.getPayload().getState())
	 * }
	 * </pre>
	 * @param destinationFunction the destination function.
	 * @param <P> the expected payload type.
	 * @return the current {@link JmsOutboundChannelAdapterSpec}.
	 * @see JmsSendingMessageHandler#setDestinationExpression
	 * @see FunctionExpression
	 */
	public <P> S destination(Function<Message<P>, ?> destinationFunction) {
		this.target.setDestinationExpression(new FunctionExpression<>(destinationFunction));
		return _this();
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code deliveryMode} for JMS message to send.
	 * @param deliveryModeExpression to use
	 * @return the spec
	 * @since 5.1
	 */
	public S deliveryModeExpression(String deliveryModeExpression) {
		this.target.setDeliveryModeExpressionString(deliveryModeExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to resolve a {@code deliveryMode} for JMS message to send.
	 * @param deliveryModeFunction to use
	 * @param <P> the expected payload type.
	 * @return the spec
	 * @since 5.1
	 * @see FunctionExpression
	 */
	public <P> S deliveryModeFunction(Function<Message<P>, ?> deliveryModeFunction) {
		this.target.setDeliveryModeExpression(new FunctionExpression<>(deliveryModeFunction));
		return _this();
	}

	/**
	 * Specify a SpEL expression to evaluate a {@code timeToLive} for JMS message to send.
	 * @param timeToLiveExpression to use
	 * @return the spec
	 * @since 5.1
	 */
	public S timeToLiveExpression(String timeToLiveExpression) {
		this.target.setTimeToLiveExpressionString(timeToLiveExpression);
		return _this();
	}

	/**
	 * Specify a {@link Function} to resolve a {@code timeToLive} for JMS message to send.
	 * @param timeToLiveFunction to use
	 * @param <P> the expected payload type.
	 * @return the spec
	 * @since 5.1
	 * @see FunctionExpression
	 */
	public <P> S timeToLiveFunction(Function<Message<P>, ?> timeToLiveFunction) {
		this.target.setTimeToLiveExpression(new FunctionExpression<>(timeToLiveFunction));
		return _this();
	}

	/**
	 * A {@link JmsTemplate}-based {@link JmsOutboundChannelAdapterSpec} extension.
	 */
	public static class JmsOutboundChannelSpecTemplateAware
			extends JmsOutboundChannelAdapterSpec<JmsOutboundChannelSpecTemplateAware>
			implements ComponentsRegistration {

		protected JmsOutboundChannelSpecTemplateAware(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		public JmsOutboundChannelSpecTemplateAware configureJmsTemplate(Consumer<JmsTemplateSpec> configurer) {
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
