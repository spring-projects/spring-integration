/*
 * Copyright 2023 the original author or authors.
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

import org.aopalliance.aop.Advice
import org.aopalliance.intercept.MethodInterceptor
import org.reactivestreams.Publisher
import org.springframework.integration.scheduling.PollerMetadata
import org.springframework.messaging.Message
import org.springframework.messaging.MessageHandler
import org.springframework.scheduling.TaskScheduler
import org.springframework.transaction.TransactionManager
import org.springframework.transaction.interceptor.TransactionInterceptor
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * A [ConsumerEndpointSpec] wrapped for Kotlin DSL.
 *
 * @property delegate the [ConsumerEndpointSpec] this instance is delegating to.
 *
 * @author Artem Bilan
 *
 * @since 5.5.19
 */
abstract class KotlinConsumerEndpointSpec<S : ConsumerEndpointSpec<S, H>, H : MessageHandler>(open val delegate: S)
	: ConsumerEndpointSpec<S, H>(delegate.handler) {

	override fun phase(phase: Int): S {
		return this.delegate.phase(phase)
	}

	override fun autoStartup(autoStartup: Boolean): S {
		return this.delegate.autoStartup(autoStartup)
	}

	override fun poller(pollerMetadata: PollerMetadata): S {
		return this.delegate.poller(pollerMetadata)
	}

	override fun reactive(): S {
		return this.delegate.reactive()
	}

	fun reactive(reactiveCustomizer: (Flux<Message<*>>) -> Publisher<Message<*>>) {
		this.delegate.reactive(reactiveCustomizer)
	}

	override fun role(role: String): S {
		return this.delegate.role(role)
	}

	override fun taskScheduler(taskScheduler: TaskScheduler): S {
		return this.delegate.taskScheduler(taskScheduler)
	}

	override fun handleMessageAdvice(vararg interceptors: MethodInterceptor?): S {
		return this.delegate.handleMessageAdvice(*interceptors)
	}

	override fun advice(vararg advice: Advice?): S {
		return this.delegate.advice(*advice)
	}

	override fun transactional(transactionManager: TransactionManager): S {
		return this.delegate.transactional(transactionManager)
	}

	override fun transactional(transactionManager: TransactionManager, handleMessageAdvice: Boolean): S {
		return this.delegate.transactional(transactionManager, handleMessageAdvice)
	}

	override fun transactional(transactionInterceptor: TransactionInterceptor): S {
		return this.delegate.transactional(transactionInterceptor)
	}

	override fun transactional(): S {
		return this.delegate.transactional()
	}

	override fun transactional(handleMessageAdvice: Boolean): S {
		return this.delegate.transactional(handleMessageAdvice)
	}

	fun <T : Any?, V : Any?> customizeMonoReply(replyCustomizer: (Message<*>, Mono<T>) -> Publisher<V>) {
		this.delegate.customizeMonoReply(replyCustomizer)
	}

	override fun id(id: String?): S {
		return this.delegate.id(id)
	}

	override fun poller(pollerMetadataSpec: PollerSpec): S {
		return this.delegate.poller(pollerMetadataSpec)
	}

	fun poller(pollers: (PollerFactory) -> PollerSpec) {
		this.delegate.poller(pollers)
	}

}
