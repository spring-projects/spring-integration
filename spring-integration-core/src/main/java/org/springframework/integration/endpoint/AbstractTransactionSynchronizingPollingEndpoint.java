/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.endpoint;

import org.springframework.integration.Message;
import org.springframework.integration.transaction.ExpressionEvaluatingTransactionSynchronizationProcessor;
import org.springframework.integration.transaction.IntegrationResourceHolder;
import org.springframework.integration.transaction.TransactionSynchronizationFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Subclasses support pollers with transaction synchronization.
 * <p/>
 * This class will be deprecated in version 3.0.0 when its methods will be pulled up
 * into {@link AbstractPollingEndpoint}.
 * @author Gary Russell
 * @since 2.2
 *
 */
public abstract class AbstractTransactionSynchronizingPollingEndpoint extends AbstractPollingEndpoint {

	private volatile TransactionSynchronizationFactory transactionSynchronizationFactory;

	public void setTransactionSynchronizationFactory(
			TransactionSynchronizationFactory transactionSynchronizationFactory) {
		this.transactionSynchronizationFactory = transactionSynchronizationFactory;
	}

	/**
	 * Return a resource (MessageSource etc) to bind when using transaction
	 * synchronization.
	 * @return The resource, or null if transaction synchronization is not required.
	 */
	protected Object getResourceToBind() {
		return null;
	}

	/**
	 * Return the key under which the resource will be made available as an
	 * attribute on the {@link IntegrationResourceHolder}. The default
	 * {@link ExpressionEvaluatingTransactionSynchronizationProcessor}
	 * makes this attribute available as a variable in SpEL expressions.
	 * @return The key, or null (default) if the resource shouldn't be
	 * made available as a attribute.
	 */
	protected String getResourceKey() {
		return null;
	}

	@Override
	protected final boolean doPoll() {
		IntegrationResourceHolder holder = bindResourceHolderIfNecessary(
				this.getResourceKey(), this.getResourceToBind());
		Message<?> message = this.doReceive();
		boolean result;
		if (message == null) {
			if (this.logger.isDebugEnabled()){
				this.logger.debug("Received no Message during the poll, returning 'false'");
			}
			result = false;
		}
		else {
			if (this.logger.isDebugEnabled()){
				this.logger.debug("Poll resulted in Message: " + message);
			}
			if (holder != null) {
				holder.setMessage(message);
			}
			this.handleMessage(message);
			result = true;
		}
		return result;
	}

	private IntegrationResourceHolder bindResourceHolderIfNecessary(String key, Object resource) {
		IntegrationResourceHolder holder = null;

		if (this.transactionSynchronizationFactory != null && resource != null) {
			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				holder = new IntegrationResourceHolder();
				if (key != null) {
					holder.addAttribute(key, resource);
				}
				TransactionSynchronizationManager.bindResource(resource, holder);
				TransactionSynchronizationManager.registerSynchronization(this.transactionSynchronizationFactory.create(resource));
			}
		}
		return holder;
	}

	/**
	 * Obtain the next message (if one is available). MAY return null
	 * if no message is immediately available.
	 * @return The message or null.
	 */
	protected abstract Message<?> doReceive();

	/**
	 * Handle a message.
	 * @param message The message.
	 */
	protected abstract void handleMessage(Message<?> message);

}
