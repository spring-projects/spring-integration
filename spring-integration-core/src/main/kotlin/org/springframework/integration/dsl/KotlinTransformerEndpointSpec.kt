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
import org.springframework.integration.core.GenericTransformer
import org.springframework.integration.handler.BeanNameMessageProcessor
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer
import org.springframework.integration.transformer.MessageTransformingHandler
import org.springframework.integration.transformer.MethodInvokingTransformer

/**
 * A [TransformerEndpointSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [TransformerEndpointSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
class KotlinTransformerEndpointSpec(override val delegate: TransformerEndpointSpec)
	: KotlinConsumerEndpointSpec<TransformerEndpointSpec, MessageTransformingHandler>(delegate) {

	/**
	 * Provide a Kotlin function as a direct delegate for [MessageTransformingHandler].
	 * @param function the function instance to use.
	 * @param <P> the input type.
	 */
	inline fun <reified P> transformer(crossinline function: (P) -> Any) {
		this.delegate.expectedType(P::class.java)
		this.delegate.transformer<P, Any> { function(it) }
	}

	/**
	 * Provide a [GenericTransformer] as a direct delegate for [MessageTransformingHandler].
	 * @param transformer the [GenericTransformer] instance to use.
	 * @param <P> the input type.
	 * @param <T> the output type.
	 */
	fun <P, T> transformer(transformer: GenericTransformer<P, T>) {
		this.delegate.transformer(transformer)
	}

	/**
	 * Provide an expression to use an [ExpressionEvaluatingTransformer] for the target handler.
	 * @param expression the SpEL expression to use.
	 */
	fun expression(expression: String) {
		this.delegate.expression(expression)
	}

	/**
	 * Provide an expression to use an [ExpressionEvaluatingTransformer] for the target handler.
	 * @param expression the SpEL expression to use.
	 */
	fun expression(expression: Expression) {
		this.delegate.expression(expression)
	}

	/**
	 * Provide a service to use a [MethodInvokingTransformer] for the target handler.
	 * @param ref the service to call as a transformer POJO.
	 */
	fun ref(ref: Any) {
		this.delegate.ref(ref)
	}

	/**
	 * Provide a bean name to use a [MethodInvokingTransformer]
	 * (based on [BeanNameMessageProcessor]) for the target handler.
	 * @param refName the bean name for service to call as a transformer POJO.
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
	 * Provide a [MessageProcessorSpec] as a factory for [MethodInvokingTransformer] delegate.
	 * @param processor the [MessageProcessorSpec] to use.
	 */
	fun processor(processor: MessageProcessorSpec<*>) {
		this.delegate.processor(processor)
	}

}
