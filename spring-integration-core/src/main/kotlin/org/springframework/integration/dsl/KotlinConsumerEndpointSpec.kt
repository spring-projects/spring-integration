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
abstract class KotlinConsumerEndpointSpec<S : ConsumerEndpointSpec<S, H>, H : MessageHandler>(open val delegate: S) {

	fun phase(phase: Int) {
		this.delegate.phase(phase)
	}

	fun autoStartup(autoStartup: Boolean) {
		this.delegate.autoStartup(autoStartup)
	}

	fun poller(pollerMetadata: PollerMetadata) {
		this.delegate.poller(pollerMetadata)
	}

	fun reactive() {
		this.delegate.reactive()
	}

	fun reactive(reactiveCustomizer: (Flux<Message<*>>) -> Publisher<Message<*>>) {
		this.delegate.reactive(reactiveCustomizer)
	}

	fun role(role: String) {
		this.delegate.role(role)
	}

	fun taskScheduler(taskScheduler: TaskScheduler) {
		this.delegate.taskScheduler(taskScheduler)
	}

	fun handleMessageAdvice(vararg interceptors: MethodInterceptor?) {
		this.delegate.handleMessageAdvice(*interceptors)
	}

	fun advice(vararg advice: Advice?) {
		this.delegate.advice(*advice)
	}

	fun transactional(transactionManager: TransactionManager) {
		this.delegate.transactional(transactionManager)
	}

	fun transactional(transactionManager: TransactionManager, handleMessageAdvice: Boolean) {
		this.delegate.transactional(transactionManager, handleMessageAdvice)
	}

	fun transactional(transactionInterceptor: TransactionInterceptor) {
		this.delegate.transactional(transactionInterceptor)
	}

	fun transactional() {
		this.delegate.transactional()
	}

	fun transactional(handleMessageAdvice: Boolean) {
		this.delegate.transactional(handleMessageAdvice)
	}

	fun <T : Any?, V : Any?> customizeMonoReply(replyCustomizer: (Message<*>, Mono<T>) -> Publisher<V>) {
		this.delegate.customizeMonoReply(replyCustomizer)
	}

	fun id(id: String?) {
		this.delegate.id(id)
	}

	fun poller(pollerMetadataSpec: PollerSpec) {
		this.delegate.poller(pollerMetadataSpec)
	}

	fun poller(pollers: (PollerFactory) -> PollerSpec) {
		this.delegate.poller(pollers)
	}

}
