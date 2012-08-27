/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
/**
*
* @author Gary Russell
* @author Oleg Zhurakousky
* @since 2.2
*
*/
public class ExpressionEvaluatingTransactionSynchronizationFactory implements
		TransactionSynchronizationFactory {

	private final Log logger = LogFactory.getLog(getClass());

	private final ExpressionEvaluatingTransactionSynchronizationProcessor processor;

	public ExpressionEvaluatingTransactionSynchronizationFactory(ExpressionEvaluatingTransactionSynchronizationProcessor processor){
		Assert.notNull(processor, "'processor' must not be null");
		this.processor = processor;
	}

	public TransactionSynchronization create(Object key) {
		Assert.notNull(key, "'key' must not be null");
		Object resourceHolder = TransactionSynchronizationManager.getResource(key);
		Assert.isInstanceOf(MessageSourceResourceHolder.class, resourceHolder);
		return new PseudoTransactionalResourceSynchronization((MessageSourceResourceHolder) resourceHolder, key);
	}

	private class PseudoTransactionalResourceSynchronization
		extends ResourceHolderSynchronization<MessageSourceResourceHolder, Object> {

		private final MessageSourceResourceHolder resourceHolder;

		public PseudoTransactionalResourceSynchronization(MessageSourceResourceHolder resourceHolder,
				Object resourceKey) {
			super(resourceHolder, resourceKey);
			this.resourceHolder = resourceHolder;
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return false;
		}

		@Override
		protected void processResourceAfterCommit(MessageSourceResourceHolder resourceHolder) {
			if (logger.isTraceEnabled()) {
				logger.trace("'Committing' pseudo-transactional resource");
			}

			processor.processAfterCommit(resourceHolder.getMessage(), resourceHolder.getResource());

			MessageSource<?> messageSource = resourceHolder.getMessageSource();
			if (messageSource instanceof PseudoTransactionalMessageSource<?,?>){
				((PseudoTransactionalMessageSource<?,?>)messageSource).afterCommit(resourceHolder.getResource());
			}
		}

		@Override
		public void afterCompletion(int status) {
			if (status != TransactionSynchronization.STATUS_COMMITTED) {
				if (logger.isTraceEnabled()) {
					logger.trace("'Rolling back' pseudo-transactional resource");
				}

				processor.processAfterRollback(resourceHolder.getMessage(), resourceHolder.getResource());

				MessageSource<?> messageSource = resourceHolder.getMessageSource();
				if (messageSource instanceof PseudoTransactionalMessageSource<?,?>){
					((PseudoTransactionalMessageSource<?,?>)messageSource).afterRollback(resourceHolder.getResource());
				}
			}
			super.afterCompletion(status);
		}
	}

}
