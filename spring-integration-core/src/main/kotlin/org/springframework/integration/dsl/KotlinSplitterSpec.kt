/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.integration.dsl

import org.springframework.expression.Expression
import org.springframework.integration.handler.BeanNameMessageProcessor
import org.springframework.integration.splitter.AbstractMessageSplitter
import org.springframework.integration.splitter.DefaultMessageSplitter
import org.springframework.integration.splitter.ExpressionEvaluatingSplitter
import org.springframework.integration.splitter.MethodInvokingSplitter
import org.springframework.messaging.MessageChannel

/**
 * A [SplitterSpec] wrapped for Kotlin DSL.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
class KotlinSplitterSpec(override val delegate: SplitterSpec)
	: KotlinConsumerEndpointSpec<SplitterSpec, AbstractMessageSplitter>(delegate) {

	/**
	 * Provide a Kotlin function as a direct delegate for
	 * [org.springframework.integration.splitter.MethodInvokingSplitter].
	 * @param function the function instance to use.
	 * @param <P> the input type.
	 */
	inline fun <reified P> function(crossinline function: (P) -> Any) {
		this.delegate.expectedType(P::class.java)
		this.delegate.function<P> { function(it) }
	}

	/**
	 * Set delimiters to tokenize String values. The default is
	 * `null` indicating that no tokenizing should occur.
	 * If delimiters are provided, they will be applied to any String payload.
	 * Only applied if provided `splitter` is instance of [DefaultMessageSplitter].
	 * @param delimiters The delimiters.
	 */
	fun delimiters(delimiters: String) {
		this.delegate.delimiters(delimiters)
	}

	/**
	 * Provide an expression to use an [ExpressionEvaluatingSplitter] for the target handler.
	 * @param expression the SpEL expression to use.
	 */
	fun expression(expression: String){
		this.delegate.expression(expression)
	}

	/**
	 * Provide an expression to use an [ExpressionEvaluatingSplitter] for the target handler.
	 * @param expression the SpEL expression to use.
	 */
	fun expression(expression: Expression) {
		this.delegate.expression(expression)
	}

	/**
	 * Provide a service to use a [MethodInvokingSplitter] for the target handler.
	 * This option can be set to an [AbstractMessageSplitter] implementation,
	 * a [MessageHandlerSpec] providing an [AbstractMessageSplitter],
	 * or [MessageProcessorSpec].
	 * @param ref the service to call as a splitter POJO.
	 */
	fun ref(ref: Any) {
		this.delegate.ref(ref)
	}

	/**
	 * Provide a bean name to use a [MethodInvokingSplitter]
	 * (based on [BeanNameMessageProcessor]) for the target handler.
	 * @param refName the bean name for service to call as a splitter POJO.
	 */
	fun refName(refName: String) {
		this.delegate.refName(refName)
	}

	/**
	 * Provide a service method name to call. Optional.
	 * Use only together with [.ref] or [.refName].
	 * @param method the service method name to call.
	 */
	fun method(method: String?) {
		this.delegate.method(method)
	}

	/**
	 * Set the applySequence flag to the specified value. Defaults to `true`.
	 * @param applySequence the applySequence.
	 */
	fun applySequence(applySequence: Boolean) {
		this.delegate.applySequence(applySequence)
	}

	/**
	 * Specify a channel where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannel The discard channel.
	 */
	fun discardChannel(discardChannel: MessageChannel) {
		this.delegate.discardChannel(discardChannel)
	}

	/**
	 * Specify a channel bean name where rejected Messages should be sent. If the discard
	 * channel is null (the default), rejected Messages will be dropped.
	 * A "Rejected Message" means that split function has returned an empty result (but not null):
	 * no items to iterate for sending.
	 * @param discardChannelName The discard channel bean name.
	 */
	fun discardChannel(discardChannelName: String) {
		this.delegate.discardChannel(discardChannelName)
	}

	/**
	 * Configure a subflow to run for discarded messages instead of a [discardChannel].
	 * @param discardFlow the discard flow.
	 */
	fun discardFlow(discardFlow: KotlinIntegrationFlowDefinition.() -> Unit) {
		this.delegate.discardFlow {definition -> discardFlow(KotlinIntegrationFlowDefinition(definition)) }
	}

}
