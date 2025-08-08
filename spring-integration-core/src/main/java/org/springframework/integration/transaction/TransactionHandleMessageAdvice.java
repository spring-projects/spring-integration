/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.transaction;

import java.util.Properties;

import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * A {@link TransactionInterceptor} extension with {@link HandleMessageAdvice} marker.
 * <p>
 * When this {@link org.aopalliance.aop.Advice}
 * is used from the {@code request-handler-advice-chain}, it is applied
 * to the {@link org.springframework.messaging.MessageHandler#handleMessage}
 * (not to the
 * {@link org.springframework.integration.handler.AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage}),
 * therefore the entire downstream process is wrapped to the transaction.
 * <p>
 * In any other cases it is operated as a regular {@link TransactionInterceptor}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
@SuppressWarnings("serial")
public class TransactionHandleMessageAdvice extends TransactionInterceptor implements HandleMessageAdvice {

	public TransactionHandleMessageAdvice() {
	}

	public TransactionHandleMessageAdvice(TransactionManager transactionManager, Properties transactionAttributes) {
		setTransactionManager(transactionManager);
		setTransactionAttributes(transactionAttributes);
	}

	public TransactionHandleMessageAdvice(TransactionManager transactionManager,
			TransactionAttributeSource transactionAttributeSource) {

		super(transactionManager, transactionAttributeSource);
	}

}
