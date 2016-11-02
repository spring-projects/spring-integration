/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.aopalliance.aop.Advice;

import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.messaging.MessageHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * A {@link EndpointSpec} for consumer endpoints.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class ConsumerEndpointSpec<S extends ConsumerEndpointSpec<S, H>, H extends MessageHandler>
		extends EndpointSpec<S, ConsumerEndpointFactoryBean, H> {

	protected final List<Advice> adviceChain = new LinkedList<>();

	protected ConsumerEndpointSpec(H messageHandler) {
		super(messageHandler);
		if (messageHandler != null) {
			this.endpointFactoryBean.setHandler(messageHandler);
		}
		this.endpointFactoryBean.setAdviceChain(this.adviceChain);
		if (messageHandler instanceof AbstractReplyProducingMessageHandler) {
			((AbstractReplyProducingMessageHandler) messageHandler).setAdviceChain(this.adviceChain);
		}
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
	 * Configure a list of {@link Advice} objects to be applied, in nested order, to the endpoint's handler.
	 * The advice objects are applied only to the handler.
	 * @param advice the advice chain.
	 * @return the endpoint spec.
	 */
	public S advice(Advice... advice) {
		this.adviceChain.addAll(Arrays.asList(advice));
		return _this();
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the
	 * provided {@code PlatformTransactionManager} and default {@link DefaultTransactionAttribute}
	 * for the {@code pollingTask}.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @return the spec.
	 */
	public S transactional(PlatformTransactionManager transactionManager) {
		return transactional(transactionManager, false);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the
	 * provided {@code PlatformTransactionManager} and default {@link DefaultTransactionAttribute}
	 * for the {@code pollingTask}.
	 * @param transactionManager the {@link PlatformTransactionManager} to use.
	 * @param handleMessageAdvice the flag to indicate the target {@link Advice} type:
	 * {@code false} - regular {@link TransactionInterceptor};
	 * {@code true} - {@link org.springframework.integration.transaction.TransactionHandleMessageAdvice} extension.
	 * @return the spec.
	 */
	public S transactional(PlatformTransactionManager transactionManager, boolean handleMessageAdvice) {
		return transactional(new TransactionInterceptorBuilder(handleMessageAdvice)
				.transactionManager(transactionManager)
				.build());
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} for the {@code pollingTask}.
	 * @param transactionInterceptor the {@link TransactionInterceptor} to use.
	 * @return the spec.
	 * @see TransactionInterceptorBuilder
	 */
	public S transactional(TransactionInterceptor transactionInterceptor) {
		return advice(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default {@code PlatformTransactionManager}
	 * and {@link DefaultTransactionAttribute} for the {@code pollingTask}.
	 * @return the spec.
	 */
	public S transactional() {
		return transactional(false);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default {@code PlatformTransactionManager}
	 * and {@link DefaultTransactionAttribute} for the {@code pollingTask}.
	 * @param handleMessageAdvice the flag to indicate the target {@link Advice} type:
	 * {@code false} - regular {@link TransactionInterceptor};
	 * {@code true} - {@link org.springframework.integration.transaction.TransactionHandleMessageAdvice} extension.
	 * @return the spec.
	 */
	public S transactional(boolean handleMessageAdvice) {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptorBuilder(handleMessageAdvice).build();
		this.componentToRegister.add(transactionInterceptor);
		return transactional(transactionInterceptor);
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
			logger.warn("'requiresReply' can be applied only for AbstractReplyProducingMessageHandler");
		}
		return _this();
	}

	/**
	 * @param sendTimeout the send timeout.
	 * @return the endpoint spec.
	 * @see AbstractReplyProducingMessageHandler#setSendTimeout(long)
	 */
	public S sendTimeout(long sendTimeout) {
		assertHandler();
		if (this.handler instanceof AbstractReplyProducingMessageHandler) {
			((AbstractReplyProducingMessageHandler) this.handler).setSendTimeout(sendTimeout);
		}
		else {
			logger.warn("'sendTimeout' can be applied only for AbstractReplyProducingMessageHandler");
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
			logger.warn("'order' can be applied only for AbstractMessageHandler");
		}
		return _this();
	}

	/**
	 * Allow async replies. If the handler reply is a {@code ListenableFuture} send
	 * the output when it is satisfied rather than sending the future as the result.
	 * Only subclasses that support this feature should set it.
	 * @param async true to allow.
	 * @return the endpoint spec.
	 * @see AbstractReplyProducingMessageHandler#setAsync(boolean)
	 */
	public S async(boolean async) {
		assertHandler();
		if (this.handler instanceof AbstractReplyProducingMessageHandler) {
			((AbstractReplyProducingMessageHandler) this.handler).setAsync(async);
		}
		else {
			logger.warn("'async' can be applied only for AbstractReplyProducingMessageHandler");
		}
		return _this();
	}

}
