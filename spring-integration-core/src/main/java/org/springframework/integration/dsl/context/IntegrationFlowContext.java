/*
 * Copyright 2018-2020 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.messaging.MessageChannel;

/**
 * A public API for dynamic (manual) registration of {@link IntegrationFlow}s,
 * not via standard bean registration phase.
 * <p>
 * The bean of this component is provided via framework automatically.
 * A bean name is based on the decapitalized class name.
 * It must be injected to the target service before use.
 * <p>
 * The typical use-case, and, therefore algorithm, is:
 * <ul>
 * <li> create an {@link IntegrationFlow} instance depending of the business logic
 * <li> register that {@link IntegrationFlow} in this {@link IntegrationFlowContext},
 * with optional {@code id} and {@code autoStartup} flag
 * <li> obtain a {@link MessagingTemplate} for that {@link IntegrationFlow}
 * (if it is started from the {@link MessageChannel}) and send (or send-and-receive)
 * messages to the {@link IntegrationFlow}
 * <li> remove the {@link IntegrationFlow} by its {@code id} from this {@link IntegrationFlowContext}
 * </ul>
 * <p>
 * For convenience an associated {@link IntegrationFlowRegistration} is returned after registration.
 * It can be used for access to the target {@link IntegrationFlow} or for manipulation with its lifecycle.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Artem Vozhdayenko
 *
 * @since 5.0
 *
 * @see IntegrationFlowRegistration
 */
public interface IntegrationFlowContext {

	/**
	 * Associate provided {@link IntegrationFlow} with an {@link IntegrationFlowRegistrationBuilder}
	 * for additional options and farther registration in the application context.
	 * @param integrationFlow the {@link IntegrationFlow} to register
	 * @return the IntegrationFlowRegistrationBuilder associated with the provided {@link IntegrationFlow}
	 */
	IntegrationFlowRegistrationBuilder registration(IntegrationFlow integrationFlow);

	/**
	 * Obtain an {@link IntegrationFlowRegistration} for the {@link IntegrationFlow}
	 * associated with the provided {@code flowId}.
	 * @param flowId the bean name to obtain
	 * @return the IntegrationFlowRegistration for provided {@code id} or {@code null}
	 */
	IntegrationFlowRegistration getRegistrationById(String flowId);

	/**
	 * Destroy an {@link IntegrationFlow} bean (as well as all its dependant beans)
	 * for provided {@code flowId} and clean up all the local cache for it.
	 * @param flowId the bean name to destroy from
	 */
	void remove(String flowId);

	/**
	 * Obtain a {@link MessagingTemplate} with its default destination set to the input channel
	 * of the {@link IntegrationFlow} for provided {@code flowId}.
	 * <p> Any {@link IntegrationFlow} bean (not only manually registered) can be used for this method.
	 * <p> If {@link IntegrationFlow} doesn't start with the {@link MessageChannel}, the
	 * {@link IllegalStateException} is thrown.
	 * @param flowId the bean name to obtain the input channel from
	 * @return the {@link MessagingTemplate} instance
	 */
	MessagingTemplate messagingTemplateFor(String flowId);

	/**
	 * Provide the state of the mapping of integration flow names to their
	 * {@link IntegrationFlowRegistration} instances.
	 * @return the registry of flow ids and their registration.
	 */
	Map<String, IntegrationFlowRegistration> getRegistry();

	/**
	 * Return true to prefix flow bean names with the flow id and a period.
	 * @param flowId the flow id.
	 * @return true to use as a prefix.
	 * @since 5.0.6
	 */
	default boolean isUseIdAsPrefix(String flowId) {
		return false;
	}

	/**
	 * @author Gary Russell
	 * @since 5.1
	 *
	 */
	interface IntegrationFlowRegistration extends BeanFactoryAware {

		/**
		 * Return the flow id.
		 * @return the id.
		 */
		String getId();

		/**
		 * Return the flow.
		 * @return the flow.
		 */
		IntegrationFlow getIntegrationFlow();

		/**
		 * Return the flow input channel.
		 * @return the channel.
		 */
		MessageChannel getInputChannel();

		/**
		 * Obtain a {@link MessagingTemplate} with its default destination set to the input channel
		 * of the {@link IntegrationFlow}.
		 * <p> Any {@link IntegrationFlow} bean (not only manually registered) can be used for this method.
		 * <p> If {@link IntegrationFlow} doesn't start with the {@link MessageChannel}, the
		 * {@link IllegalStateException} is thrown.
		 * @return the {@link MessagingTemplate} instance
		 */
		MessagingTemplate getMessagingTemplate();

		/**
		 * Start the registration.
		 */
		void start();

		/**
		 * Stop the registration.
		 */
		void stop();

		/**
		 * Destroy the {@link IntegrationFlow} bean (as well as all its dependent beans)
		 * and clean up all the local cache for it.
		 */
		void destroy();

	}

	/**
	 * A Builder pattern implementation for the options to register {@link IntegrationFlow}
	 * in the application context.
	 */
	interface IntegrationFlowRegistrationBuilder {

		/**
		 * Specify an {@code id} for the {@link IntegrationFlow} to register.
		 * Must be unique per context.
		 * The registration with this {@code id} must be destroyed before reusing for
		 * a new {@link IntegrationFlow} instance.
		 * @param id the id for the {@link IntegrationFlow} to register
		 * @return the current builder instance
		 */
		IntegrationFlowRegistrationBuilder id(String id);

		/**
		 * The {@code boolean} flag to indication if an {@link IntegrationFlow} must be started
		 * automatically after registration. Defaults to {@code true}.
		 * @param autoStartup start or not the {@link IntegrationFlow} automatically after registration.
		 * @return the current builder instance
		 */
		IntegrationFlowRegistrationBuilder autoStartup(boolean autoStartup);

		/**
		 * Add an object which will be registered as an {@link IntegrationFlow} dependant bean in the
		 * application context. Usually it is some support component, which needs an application context.
		 * For example dynamically created connection factories or header mappers for AMQP, JMS, TCP etc.
		 * @param bean an additional arbitrary bean to register into the application context.
		 * @return the current builder instance
		 */
		IntegrationFlowRegistrationBuilder addBean(Object bean);

		/**
		 * Add an object which will be registered as an {@link IntegrationFlow} dependant bean in the
		 * application context. Usually it is some support component, which needs an application context.
		 * For example dynamically created connection factories or header mappers for AMQP, JMS, TCP etc.
		 * @param name the name for the bean to register.
		 * @param bean an additional arbitrary bean to register into the application context.
		 * @return the current builder instance
		 */
		IntegrationFlowRegistrationBuilder addBean(String name, Object bean);

		/**
		 * Set the configuration source {@code Object} for this manual Integration flow definition.
		 * Can be any arbitrary object which could easily lead to a source code for the flow when
		 * a messaging exception happens at runtime.
		 * @param source the configuration source representation.
		 * @return the current builder instance
		 * @since 5.2
		 */
		IntegrationFlowRegistrationBuilder setSource(Object source);

		/**
		 * Invoke this method to prefix bean names in the flow with the (required) flow id
		 * and a period. This is useful if you wish to register the same flow multiple times
		 * while retaining the ability to reference beans within the flow; adding the unique
		 * flow id to the bean name makes the name unique.
		 * @return the current builder instance.
		 * @see #id(String)
		 * @since 5.0.6
		 */
		default IntegrationFlowRegistrationBuilder useFlowIdAsPrefix() {
			return this;
		}

		/**
		 * Register an {@link IntegrationFlow} and all the dependant and support components
		 * in the application context and return an associated {@link IntegrationFlowRegistration}
		 * control object.
		 * @return the {@link IntegrationFlowRegistration} instance.
		 */
		IntegrationFlowRegistration register();

	}

}
