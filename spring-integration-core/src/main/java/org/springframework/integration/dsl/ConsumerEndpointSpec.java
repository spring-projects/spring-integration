/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.reactivestreams.Publisher;

import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.HandleMessageAdviceAdapter;
import org.springframework.integration.handler.advice.ReactiveRequestHandlerAdvice;
import org.springframework.integration.router.AbstractMessageRouter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * A {@link EndpointSpec} for consumer endpoints.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class ConsumerEndpointSpec<S extends ConsumerEndpointSpec<S, H>, H extends MessageHandler>
		extends EndpointSpec<S, ConsumerEndpointFactoryBean, H> {

	protected final List<Advice> adviceChain = new LinkedList<>(); // NOSONAR final

	protected ConsumerEndpointSpec(H messageHandler) {
		super(messageHandler, new ConsumerEndpointFactoryBean());
	}

	@Override
	public S phase(int phase) {
		this.endpointFactoryBean.setPhase(phase);
		return _this();
	}

	@Override
	public S autoStartup(boolean autoStartup) {
		this.endpointFactoryBean.setAutoStartup(autoStartup);
		return _this();
	}

	@Override
	public S poller(PollerMetadata pollerMetadata) {
		this.endpointFactoryBean.setPollerMetadata(pollerMetadata);
		return _this();
	}

	/**
	 * Make the consumer endpoint as reactive independently of an input channel.
	 * @return the spec
	 * @since 5.5
	 */
	public S reactive() {
		return reactive(Function.identity());
	}
	/**
	 * Make the consumer endpoint as reactive independently of an input channel and
	 * apply the provided function into the {@link Flux#transform(Function)} operator.
	 * @param reactiveCustomizer the function to transform {@link Flux} for the input channel.
	 * @return the spec
	 * @since 5.5
	 */
	public S reactive(Function<? super Flux<Message<?>>, ? extends Publisher<Message<?>>> reactiveCustomizer) {
		this.endpointFactoryBean.setReactiveCustomizer(reactiveCustomizer);
		return _this();
	}

	@Override
	public S role(String role) {
		this.endpointFactoryBean.setRole(role);
		return _this();
	}

	/**
	 * Configure a {@link TaskScheduler} for scheduling tasks, for example in the
	 * Polling Consumer. By default the global {@code ThreadPoolTaskScheduler} bean is used.
	 * This configuration is useful when there are requirements to dedicate particular threads
	 * for polling task, for example.
	 * @param taskScheduler the {@link TaskScheduler} to use.
	 * @return the endpoint spec.
	 * @see org.springframework.integration.context.IntegrationContextUtils#getTaskScheduler
	 */
	public S taskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "'taskScheduler' must not be null");
		this.endpointFactoryBean.setTaskScheduler(taskScheduler);
		return _this();
	}

	/**
	 * Configure a list of {@link MethodInterceptor} objects to be applied, in nested order, to the
	 * endpoint's handler. The advice objects are applied to the {@code handleMessage()} method
	 * and therefore to the whole sub-flow afterwards.
	 * @param interceptors the advice chain.
	 * @return the endpoint spec.
	 * @since 5.3
	 */
	public S handleMessageAdvice(MethodInterceptor... interceptors) {
		for (MethodInterceptor interceptor: interceptors) {
			advice(new HandleMessageAdviceAdapter(interceptor));
		}
		return _this();
	}

	/**
	 * Configure a list of {@link Advice} objects to be applied, in nested order, to the
	 * endpoint's handler. The advice objects are applied only to the handler.
	 * @param advice the advice chain.
	 * @return the endpoint spec.
	 */
	public S advice(Advice... advice) {
		this.adviceChain.addAll(Arrays.asList(advice));
		return _this();
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the provided
	 * {@code PlatformTransactionManager} and default
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the {@link MessageHandler}.
	 * @param transactionManager the {@link TransactionManager} to use.
	 * @return the spec.
	 */
	public S transactional(TransactionManager transactionManager) {
		return transactional(transactionManager, false);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the provided
	 * {@code PlatformTransactionManager} and default
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the {@link MessageHandler}.
	 * @param transactionManager the {@link TransactionManager} to use.
	 * @param handleMessageAdvice the flag to indicate the target {@link Advice} type:
	 * {@code false} - regular {@link TransactionInterceptor}; {@code true} -
	 * {@link org.springframework.integration.transaction.TransactionHandleMessageAdvice}
	 * extension.
	 * @return the spec.
	 */
	public S transactional(TransactionManager transactionManager, boolean handleMessageAdvice) {
		return transactional(new TransactionInterceptorBuilder(handleMessageAdvice)
				.transactionManager(transactionManager)
				.build());
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} for the {@link MessageHandler}.
	 * @param transactionInterceptor the {@link TransactionInterceptor} to use.
	 * @return the spec.
	 * @see TransactionInterceptorBuilder
	 */
	public S transactional(TransactionInterceptor transactionInterceptor) {
		return advice(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default
	 * {@code PlatformTransactionManager} and
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the
	 * {@link MessageHandler}.
	 * @return the spec.
	 */
	public S transactional() {
		return transactional(false);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default
	 * {@code PlatformTransactionManager} and
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the {@link MessageHandler}.
	 * @param handleMessageAdvice the flag to indicate the target {@link Advice} type:
	 * {@code false} - regular {@link TransactionInterceptor}; {@code true} -
	 * {@link org.springframework.integration.transaction.TransactionHandleMessageAdvice}
	 * extension.
	 * @return the spec.
	 */
	public S transactional(boolean handleMessageAdvice) {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptorBuilder(handleMessageAdvice).build();
		this.componentsToRegister.put(transactionInterceptor, null);
		return transactional(transactionInterceptor);
	}

	/**
	 * Specify a {@link BiFunction} for customizing {@link Mono} replies via {@link ReactiveRequestHandlerAdvice}.
	 * @param replyCustomizer the {@link BiFunction} to propagate into {@link ReactiveRequestHandlerAdvice}.
	 * @return the spec.
	 * @since 5.3
	 * @see ReactiveRequestHandlerAdvice
	 */
	public S customizeMonoReply(BiFunction<Message<?>, Mono<?>, Publisher<?>> replyCustomizer) {
		return advice(new ReactiveRequestHandlerAdvice(replyCustomizer));
	}

	/**
	 * @param requiresReply the requiresReply.
	 * @return the endpoint spec.
	 * @see AbstractReplyProducingMessageHandler#setRequiresReply(boolean)
	 */
	public S requiresReply(boolean requiresReply) {
		assertHandler();
		if (this.handler instanceof AbstractReplyProducingMessageHandler) {
			((AbstractReplyProducingMessageHandler) this.handler).setRequiresReply(requiresReply);
		}
		else {
			this.logger.warn("'requiresReply' can be applied only for AbstractReplyProducingMessageHandler");
		}
		return _this();
	}

	/**
	 * @param sendTimeout the send timeout.
	 * @return the endpoint spec.
	 * @see AbstractMessageProducingHandler#setSendTimeout(long)
	 */
	public S sendTimeout(long sendTimeout) {
		assertHandler();
		if (this.handler instanceof AbstractMessageProducingHandler) {
			((AbstractMessageProducingHandler) this.handler).setSendTimeout(sendTimeout);
		}
		else if (this.handler instanceof AbstractMessageRouter) {
			// This should probably go on the RouterSpec, but we put it here for consistency
			((AbstractMessageRouter) this.handler).setSendTimeout(sendTimeout);
		}
		else {
			this.logger.warn("'sendTimeout' can be applied only for AbstractMessageProducingHandler");
		}
		return _this();
	}

	/**
	 * @param order the order.
	 * @return the endpoint spec.
	 * @see AbstractMessageHandler#setOrder(int)
	 */
	public S order(int order) {
		assertHandler();
		if (this.handler instanceof AbstractMessageHandler) {
			((AbstractMessageHandler) this.handler).setOrder(order);
		}
		else {
			this.logger.warn("'order' can be applied only for AbstractMessageHandler");
		}
		return _this();
	}

	/**
	 * Allow async replies. If the handler reply is a
	 * {@code org.springframework.util.concurrent.ListenableFuture}, send the output when
	 * it is satisfied rather than sending the future as the result. Ignored for handler
	 * return types other than
	 * {@link org.springframework.util.concurrent.ListenableFuture}.
	 * @param async true to allow.
	 * @return the endpoint spec.
	 * @see AbstractMessageProducingHandler#setAsync(boolean)
	 */
	public S async(boolean async) {
		assertHandler();
		if (this.handler instanceof AbstractMessageProducingHandler) {
			((AbstractMessageProducingHandler) this.handler).setAsync(async);
		}
		else {
			this.logger.warn("'async' can be applied only for AbstractMessageProducingHandler");
		}
		return _this();
	}

	/**
	 * Set header patterns ("xxx*", "*xxx", "*xxx*" or "xxx*yyy")
	 * that will NOT be copied from the inbound message.
	 * At least one pattern as "*" means do not copy headers at all.
	 * @param headerPatterns the headers to not propagate from the inbound message.
	 * @return the endpoint spec.
	 * @see AbstractMessageProducingHandler#setNotPropagatedHeaders(String...)
	 */
	public S notPropagatedHeaders(String... headerPatterns) {
		assertHandler();
		if (this.handler instanceof AbstractMessageProducingHandler) {
			((AbstractMessageProducingHandler) this.handler).setNotPropagatedHeaders(headerPatterns);
		}
		else {
			this.logger.warn("'headerPatterns' can be applied only for AbstractMessageProducingHandler");
		}
		return _this();
	}

	@Override
	protected Tuple2<ConsumerEndpointFactoryBean, H> doGet() {
		this.endpointFactoryBean.setAdviceChain(this.adviceChain);
		if (this.handler instanceof AbstractReplyProducingMessageHandler && !this.adviceChain.isEmpty()) {
			((AbstractReplyProducingMessageHandler) this.handler).setAdviceChain(this.adviceChain);
		}
		this.endpointFactoryBean.setHandler(this.handler);
		return super.doGet();
	}

}
