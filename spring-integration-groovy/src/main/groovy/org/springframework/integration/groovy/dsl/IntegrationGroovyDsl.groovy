/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.groovy.dsl

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.reactivestreams.Publisher
import org.springframework.integration.core.MessageSource
import org.springframework.integration.dsl.GatewayProxySpec
import org.springframework.integration.dsl.IntegrationFlow
import org.springframework.integration.dsl.IntegrationFlowBuilder
import org.springframework.integration.dsl.IntegrationFlowDefinition
import org.springframework.integration.dsl.MessageProducerSpec
import org.springframework.integration.dsl.MessageSourceSpec
import org.springframework.integration.dsl.MessagingGatewaySpec
import org.springframework.integration.dsl.SourcePollingChannelAdapterSpec
import org.springframework.integration.endpoint.MessageProducerSupport
import org.springframework.integration.gateway.MessagingGatewaySupport
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel

import java.util.function.Consumer
import java.util.function.Supplier

/**
 * The factory class for Spring Integration Groovy DSL closures.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
@CompileStatic
class IntegrationGroovyDsl {

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL.
	 * @param flow the {@link Closure} for {@link IntegrationFlowDefinition}
	 */
	static IntegrationFlow integrationFlow(
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		{ IntegrationFlowDefinition flowDefinition ->
			def delegate = new GroovyIntegrationFlowDefinition(flowDefinition)
			flow.delegate = delegate
			flow.resolveStrategy = Closure.DELEGATE_FIRST
			flow(delegate)
		} as IntegrationFlow
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(Class, Consumer)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			Class<?> serviceInterface,
			@DelegatesTo(value = GatewayProxySpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.dsl.GatewayProxySpec')
					Closure<?> gatewaySpec = null,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		Consumer<GatewayProxySpec> configurer = GroovyIntegrationFlowDefinition.createConfigurerIfAny(gatewaySpec)
		buildIntegrationFlow(IntegrationFlow.from(serviceInterface, configurer), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(String, boolean)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			String channelName,
			Boolean fixedSubscriber = false,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(channelName, fixedSubscriber), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(MessageChannel)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessageChannel channel,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(channel), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(MessageSource, Consumer)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessageSource<?> messageSource,
			@DelegatesTo(value = SourcePollingChannelAdapterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = "org.springframework.integration.dsl.SourcePollingChannelAdapterSpec")
					Closure<?> adapterSpec = null,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		Consumer<SourcePollingChannelAdapterSpec> configurer =
				GroovyIntegrationFlowDefinition.createConfigurerIfAny(adapterSpec)
		buildIntegrationFlow(IntegrationFlow.from(messageSource, configurer), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(org.springframework.integration.dsl.MessageSourceSpec, Consumer)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessageSourceSpec messageSourceSpec,
			@DelegatesTo(value = SourcePollingChannelAdapterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = "org.springframework.integration.dsl.SourcePollingChannelAdapterSpec")
					Closure<?> adapterSpec = null,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		Consumer<SourcePollingChannelAdapterSpec> configurer =
				GroovyIntegrationFlowDefinition.createConfigurerIfAny(adapterSpec)
		buildIntegrationFlow(IntegrationFlow.from(messageSourceSpec, configurer), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#fromSupplier(Supplier, Consumer)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			Closure<Object> source,
			@DelegatesTo(value = SourcePollingChannelAdapterSpec, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.dsl.SourcePollingChannelAdapterSpec')
					Closure<?> adapterSpec = null,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		Consumer<SourcePollingChannelAdapterSpec> configurer =
				GroovyIntegrationFlowDefinition.createConfigurerIfAny(adapterSpec)
		buildIntegrationFlow(IntegrationFlow.fromSupplier(source, configurer), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(Publisher)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			Publisher<? extends Message<?>> publisher,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(publisher), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(MessagingGatewaySupport)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessagingGatewaySupport gateway,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(gateway), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(org.springframework.integration.dsl.MessagingGatewaySpec)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessagingGatewaySpec gatewaySpec,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(gatewaySpec), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(MessageProducerSupport)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessageProducerSupport producer,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(producer), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(org.springframework.integration.dsl.MessageProducerSpec)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			MessageProducerSpec producerSpec,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(producerSpec), flow)
	}

	/**
	 * Functional {@link IntegrationFlow} definition in Groovy DSL for
	 * {@link IntegrationFlow#from(IntegrationFlow)} factory method.
	 */
	static IntegrationFlow integrationFlow(
			IntegrationFlow sourceFlow,
			@DelegatesTo(value = GroovyIntegrationFlowDefinition, strategy = Closure.DELEGATE_FIRST)
			@ClosureParams(value = SimpleType,
					options = 'org.springframework.integration.groovy.dsl.GroovyIntegrationFlowDefinition')
					Closure<?> flow) {

		buildIntegrationFlow(IntegrationFlow.from(sourceFlow), flow)
	}

	private static IntegrationFlow buildIntegrationFlow(IntegrationFlowBuilder flowBuilder, Closure<?> flow) {
		flow.delegate = new GroovyIntegrationFlowDefinition(flowBuilder)
		flow.resolveStrategy = Closure.DELEGATE_FIRST
		flow()
		flowBuilder.get()
	}

	private IntegrationGroovyDsl() {
	}

}
