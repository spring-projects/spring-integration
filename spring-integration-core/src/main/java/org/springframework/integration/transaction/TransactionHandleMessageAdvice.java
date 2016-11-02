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

package org.springframework.integration.transaction;

import java.util.Properties;

import org.aopalliance.aop.Advice;

import org.springframework.integration.handler.advice.HandleMessageAdvice;
import org.springframework.messaging.MessageHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

/**
 * A {@link TransactionInterceptor} extension with {@link HandleMessageAdvice} marker.
 * <p>
 * When this {@link Advice} is used from the {@code request-handler-advice-chain}, it is applied
 * to the {@link MessageHandler#handleMessage}
 * (not to the
 * {@link org.springframework.integration.handler.AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage}),
 * therefore the entire downstream process is wrapped to the transaction.
 * <p>
 * In any other cases it is operated as a regular {@link TransactionInterceptor}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SuppressWarnings("serial")
public class TransactionHandleMessageAdvice extends TransactionInterceptor implements HandleMessageAdvice {

	public TransactionHandleMessageAdvice() {
	}

	public TransactionHandleMessageAdvice(PlatformTransactionManager ptm, Properties attributes) {
		super(ptm, attributes);
	}

	public TransactionHandleMessageAdvice(PlatformTransactionManager ptm, TransactionAttributeSource tas) {
		super(ptm, tas);
	}

}
