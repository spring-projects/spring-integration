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
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.transaction.support.ResourceHolder;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * A Channel Adapter implementation for connecting a
 * {@link MessageSource} to a {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class SourcePollingChannelAdapter extends AbstractPollingEndpoint implements TrackableComponent {

	private volatile MessageSource<?> source;

	private volatile boolean isPseudoTxMessageSource;

	private volatile MessageChannel outputChannel;

	private volatile boolean shouldTrack;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile boolean synchronizedTx = true;

	/**
	 * Specify the source to be polled for Messages.
	 */
	public void setSource(MessageSource<?> source) {
		this.source = source;
		this.isPseudoTxMessageSource = this.source instanceof PseudoTransactionalMessageSource;
	}

	/**
	 * Specify the {@link MessageChannel} where Messages should be sent.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Specify the maximum time to wait for a Message to be sent to the
	 * output channel.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	/**
	 * Specify whether this component should be tracked in the Message History.
	 */
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	public void setSynchronized(boolean synchronizedTx) {
		this.synchronizedTx = synchronizedTx;
	}

	@Override
	public String getComponentType() {
		return (this.source instanceof NamedComponent) ?
				((NamedComponent) this.source).getComponentType() : "inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
		Assert.notNull(this.source, "source must not be null");
		Assert.notNull(this.outputChannel, "outputChannel must not be null");
		super.onInit();
	}

	@Override
	protected boolean doPoll() {
		boolean isInTx = false;
		PseudoTransactionalMessageSource<?,?> messageSource = null;
		Object resource = null;
		if (this.isPseudoTxMessageSource) {
			messageSource = (PseudoTransactionalMessageSource<?,?>) this.source;
			resource = messageSource.getResource();
			Assert.state(resource != null, "Pseudo Transactional Message Source returned null resource");
			if (this.synchronizedTx && TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionSynchronizationManager.bindResource(messageSource, resource);
				TransactionSynchronizationManager.registerSynchronization(
					new PseudoTransactionalResourceSynchronization(
						new PseudoTransactionalResourceHolder(resource), this.source));
				isInTx = true;
			}
		}
		Message<?> message;
		try {
			message = this.source.receive();
		}
		finally {
			if (this.isPseudoTxMessageSource && !isInTx) {
				messageSource.afterReceiveNoTx(resource);
			}
		}
		if (this.logger.isDebugEnabled()){
			this.logger.debug("Poll resulted in Message: " + message);
		}
		if (message != null) {
			if (this.shouldTrack) {
				message = MessageHistory.write(message, this);
			}
			this.messagingTemplate.send(this.outputChannel, message);
			if (this.isPseudoTxMessageSource && !isInTx) {
				messageSource.afterSendNoTx(resource);
			}
			return true;
		}
		if (this.logger.isDebugEnabled()){
			this.logger.debug("Received no Message during the poll, returning 'false'");
		}
		return false;
	}

	private class PseudoTransactionalResourceHolder implements ResourceHolder {

		private final Object resource;

		public PseudoTransactionalResourceHolder(Object resource) {
			this.resource = resource;
		}

		protected Object getResource() {
			return resource;
		}

		public void reset() {
		}

		public void unbound() {
		}

		public boolean isVoid() {
			return false;
		}

	}

	private class PseudoTransactionalResourceSynchronization
		extends ResourceHolderSynchronization<PseudoTransactionalResourceHolder, Object> {

		private final PseudoTransactionalResourceHolder resourceHolder;

		public PseudoTransactionalResourceSynchronization(PseudoTransactionalResourceHolder resourceHolder,
				Object resourceKey) {
			super(resourceHolder, resourceKey);
			this.resourceHolder = resourceHolder;
		}

		@Override
		protected boolean shouldReleaseBeforeCompletion() {
			return false;
		}

		@Override
		protected void processResourceAfterCommit(PseudoTransactionalResourceHolder resourceHolder) {
			if (logger.isTraceEnabled()) {
				logger.trace("'Committing' pseudo-transactional resource");
			}
			((PseudoTransactionalMessageSource<?,?>) source).afterCommit(resourceHolder.getResource());
		}

		@Override
		public void afterCompletion(int status) {
			if (status != TransactionSynchronization.STATUS_COMMITTED) {
				if (logger.isTraceEnabled()) {
					logger.trace("'Rolling back' pseudo-transactional resource");
				}
				((PseudoTransactionalMessageSource<?,?>) source).afterRollback(this.resourceHolder.getResource());
			}
			super.afterCompletion(status);
		}



	}
}
