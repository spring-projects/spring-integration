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

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.NamedComponent;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ExpressionUtils;
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

	/**
	 * Transaction synchronization needs a non-null resource; this constant is used for
	 * message sources that have no need for a resource, because the post-process
	 * action just needs the message.
	 */
	private static final Object NO_TX_RESOURCE = new Object();

	private volatile MessageSource<?> source;

	private volatile boolean isPseudoTxMessageSource;

	private volatile MessageChannel outputChannel;

	private volatile boolean shouldTrack;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile Expression onSuccessExpression;

	private final MessagingTemplate onSuccessMessagingTemplate = new MessagingTemplate();

	private volatile Expression onFailureExpression;

	private final MessagingTemplate onFailureMessagingTemplate = new MessagingTemplate();

	private volatile StandardEvaluationContext evaluationContext;

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


	public void setOnSuccessExpression(Expression onSuccessExpression) {
		Assert.notNull(onSuccessExpression, "onSuccessExpression cannot be null");
		this.onSuccessExpression = onSuccessExpression;
	}

	public void setOnFailureExpression(Expression onFailureExpression) {
		Assert.notNull(onFailureExpression, "onFailureExpression cannot be null");
		this.onFailureExpression = onFailureExpression;
	}

	public void setOnSuccessResultChannel(MessageChannel onSuccessResultChannel) {
		Assert.notNull(onSuccessResultChannel, "onSuccessChannel cannot be null");
		this.onSuccessMessagingTemplate.setDefaultChannel(onSuccessResultChannel);
	}

	public void setOnFailureChannel(MessageChannel onFailureResultChannel) {
		Assert.notNull(onFailureResultChannel, "onFailureChannel cannot be null");
		this.onFailureMessagingTemplate.setDefaultChannel(onFailureResultChannel);
	}

	public void setResultSendTimeout(long sendTimeout) {
		this.onSuccessMessagingTemplate.setSendTimeout(sendTimeout);
		this.onFailureMessagingTemplate.setSendTimeout(sendTimeout);
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
		this.evaluationContext = this.createEvaluationContext();
		super.onInit();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected boolean doPoll() {
		boolean isInTx = false;
		PseudoTransactionalMessageSource<?,Object> messageSource = null;
		Object resource = NO_TX_RESOURCE;
		if (this.isPseudoTxMessageSource) {
			messageSource = (PseudoTransactionalMessageSource<?,Object>) this.source;
		}
		Message<?> message;
		try {
			message = this.source.receive();
			if (this.isPseudoTxMessageSource) {
				Object actualResource = messageSource.getResource();
				resource = actualResource != null ? actualResource : resource;
			}

			if (TransactionSynchronizationManager.isActualTransactionActive()) {
				TransactionSynchronizationManager.bindResource(this, resource);
				TransactionSynchronizationManager.registerSynchronization(
					new PseudoTransactionalResourceSynchronization(
						new PseudoTransactionalResourceHolder(message, resource), this));
				isInTx = true;
			}
		}
		finally {
			if (!isInTx && this.isPseudoTxMessageSource) {
				/*
				 * This callback is provided for 'legacy' message sources
				 * that used to take action after the receive and before
				 * the send. When in a transaction, that action is now
				 * taken after the commit but, when not in a transaction
				 * this callback provides backwards compatibility. An
				 * example is the mail reader that deletes from the inbox.
				 */
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
			try {
				this.messagingTemplate.send(this.outputChannel, message);
				if (!isInTx) {
					if (this.isPseudoTxMessageSource) {
						/*
						 * For 'legacy' message sources that need more flexibility than simple
						 * expression evaluation, we invoke this callback after a
						 * successful send.
						 */
						messageSource.afterSendNoTx(resource);
					}
					this.onSuccess(message, resource);
				}
			}
			catch (Exception e) {
				if (!isInTx) {
					this.onFailure(message, resource);
				}
				if (e instanceof MessagingException) {
					throw (MessagingException) e;
				}
				else {
					throw new MessagingException(message, e);
				}
			}
			return true;
		}
		if (this.logger.isDebugEnabled()){
			this.logger.debug("Received no Message during the poll, returning 'false'");
		}
		return false;
	}

	private void onSuccess(Message<?> message, Object resource) {
		doPostProcess(message, resource, this.onSuccessExpression, this.onSuccessMessagingTemplate, "success");
	}

	private void onFailure(Message<?> message, Object resource) {
		doPostProcess(message, resource, this.onFailureExpression, this.onFailureMessagingTemplate, "failure");
	}

	private void doPostProcess(Message<?> message, Object resource, Expression expression,
			MessagingTemplate messagingTemplate, String expressionType) {
		if (expression != null && message != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Evaluating " + expressionType + " expression: '" + expression.getExpressionString() + "' on " + message);
			}
			StandardEvaluationContext evaluationContextToUse = this.determineEvaluationContextToUse(resource);
			Object value;
			try {
				value = expression.getValue(evaluationContextToUse, message);
			}
			catch (Exception e) {
				value = e;
			}
			if (value != null) {
				try {
					messagingTemplate.send(MessageBuilder.fromMessage(message)
							.setHeader(MessageHeaders.DISPOSITION_RESULT, value).build());
				}
				catch (Exception e) {
					logger.error("Failed to send " + expressionType + " evaluation result " + message, e);
				}
			}
		}
	}

	/**
	 * If we don't need a resource variable (not a {@link PseudoTransactionalMessageSource})
	 * we can use a singleton context; otherwise we need a new one each time.
	 * @param resource The resource
	 * @return The context.
	 */
	private StandardEvaluationContext determineEvaluationContextToUse(Object resource) {
		StandardEvaluationContext evaluationContextToUse;
		if (resource != NO_TX_RESOURCE) {
			evaluationContextToUse = this.createEvaluationContext();
			evaluationContextToUse.setVariable("resource", resource);
		}
		else {
			if (this.evaluationContext == null) {
				this.evaluationContext = this.createEvaluationContext();
			}
			evaluationContextToUse = this.evaluationContext;
		}
		return evaluationContextToUse;
	}

	protected StandardEvaluationContext createEvaluationContext(){
		if (this.getBeanFactory() != null) {
			return ExpressionUtils.createStandardEvaluationContext(new BeanFactoryResolver(this.getBeanFactory()),
					this.getConversionService());
		}
		else {
			return ExpressionUtils.createStandardEvaluationContext(this.getConversionService());
		}
	}

	private class PseudoTransactionalResourceHolder implements ResourceHolder {

		private final Message<?> message;
		private final Object resource;

		public PseudoTransactionalResourceHolder(Message<?> message, Object resource) {
			this.message = message;
			this.resource = resource;
		}

		protected Object getResource() {
			return resource;
		}

		public Message<?> getMessage() {
			return message;
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
			if (isPseudoTxMessageSource) {
				((PseudoTransactionalMessageSource<?, ?>) source).afterCommit(resourceHolder.getResource());
			}
			onSuccess(resourceHolder.getMessage(), resourceHolder.getResource());
		}

		@Override
		public void afterCompletion(int status) {
			if (status != TransactionSynchronization.STATUS_COMMITTED) {
				if (logger.isTraceEnabled()) {
					logger.trace("'Rolling back' pseudo-transactional resource");
				}
				if (isPseudoTxMessageSource) {
					((PseudoTransactionalMessageSource<?, ?>) source).afterRollback(resourceHolder.getResource());
				}
				onFailure(this.resourceHolder.getMessage(), this.resourceHolder.getResource());
			}
			super.afterCompletion(status);
		}

	}
}
