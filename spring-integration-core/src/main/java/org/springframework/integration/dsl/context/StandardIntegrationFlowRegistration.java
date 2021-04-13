/*
 * Copyright 2018-2021 the original author or authors.
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

package org.springframework.integration.dsl.context;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext.IntegrationFlowRegistration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Instances of this classes are returned as a result of
 * {@link StandardIntegrationFlowContext#registration(IntegrationFlow)} invocation
 * and provide an API for some useful {@link IntegrationFlow} options and its lifecycle.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.1
 *
 * @see IntegrationFlowContext
 */
class StandardIntegrationFlowRegistration implements IntegrationFlowRegistration {

	private final IntegrationFlow integrationFlow;

	private final IntegrationFlowContext integrationFlowContext;

	private final String id;

	private MessageChannel inputChannel;

	private MessagingTemplate messagingTemplate;

	private ConfigurableListableBeanFactory beanFactory;

	StandardIntegrationFlowRegistration(IntegrationFlow integrationFlow, IntegrationFlowContext integrationFlowContext,
			String id) {

		this.integrationFlow = integrationFlow;
		this.integrationFlowContext = integrationFlowContext;
		this.id = id;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public IntegrationFlow getIntegrationFlow() {
		return this.integrationFlow;
	}

	@Override
	public MessageChannel getInputChannel() {
		if (this.inputChannel == null) {
			this.inputChannel = this.integrationFlow.getInputChannel();
			if (this.inputChannel == null) {
				throw new IllegalStateException("Only 'IntegrationFlow' instances started from the 'MessageChannel' " +
						"(e.g. extracted from 'IntegrationFlow' Lambdas) can be used " +
						"for direct 'send' operation. " +
						"But [" + this.integrationFlow + "] isn't one of them.\n" +
						"Consider 'BeanFactory.getBean()' usage for sending messages " +
						"to the required 'MessageChannel'.");
			}
		}
		return this.inputChannel;
	}

	/**
	 * Obtain a {@link MessagingTemplate} with its default destination set to the input channel
	 * of the {@link IntegrationFlow}.
	 * <p> Any {@link IntegrationFlow} bean (not only manually registered) can be used for this method.
	 * <p> If {@link IntegrationFlow} doesn't start with the {@link MessageChannel}, the
	 * {@link IllegalStateException} is thrown.
	 * @return the {@link MessagingTemplate} instance
	 */
	@Override
	public MessagingTemplate getMessagingTemplate() {
		if (this.messagingTemplate == null) {
			this.messagingTemplate = new MessagingTemplate(getInputChannel()) {

				@Override
				public Message<?> receive() {
					return receiveAndConvert(Message.class);
				}

				@Override
				public <T> T receiveAndConvert(Class<T> targetClass) {
					throw new UnsupportedOperationException("The 'receive()/receiveAndConvert()' " +
							"isn't supported on the 'IntegrationFlow' input channel.");
				}

			};
			this.messagingTemplate.setBeanFactory(this.beanFactory);
		}
		return this.messagingTemplate;
	}

	@Override
	public void start() {
		if (this.integrationFlow instanceof Lifecycle) {
			((Lifecycle) this.integrationFlow).start();
		}
		else {
			throw new IllegalStateException("For 'autoStartup' mode the 'IntegrationFlow' " +
					"must be an instance of 'Lifecycle'.\n" +
					"Consider to implement it for [" + this.integrationFlow + "]. " +
					"Or start dependent components on their own.");
		}
	}

	@Override
	public void stop() {
		if (this.integrationFlow instanceof Lifecycle) {
			((Lifecycle) this.integrationFlow).stop();
		}
	}

	/**
	 * Destroy the {@link IntegrationFlow} bean (as well as all its dependant beans)
	 * and clean up all the local cache for it.
	 */
	@Override
	public void destroy() {
		this.integrationFlowContext.remove(this.id);
	}

	@Override
	public String toString() {
		return "IntegrationFlowRegistration{integrationFlow=" + this.integrationFlow +
				", id='" + this.id + '\'' +
				", inputChannel=" + this.inputChannel +
				'}';
	}

}
