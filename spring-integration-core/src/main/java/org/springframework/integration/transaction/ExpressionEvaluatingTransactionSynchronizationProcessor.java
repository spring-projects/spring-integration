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

import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.util.ExpressionUtils;
import org.springframework.util.Assert;
/**
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class ExpressionEvaluatingTransactionSynchronizationProcessor extends IntegrationObjectSupport {

	private volatile StandardEvaluationContext evaluationContext;

	private volatile Expression beforeCommitExpression;

	private volatile Expression afterCommitExpression;

	private volatile Expression afterRollbackExpression;

	private volatile MessageChannel beforeCommitResultChannel;

	private volatile MessageChannel afterCommitResultChannel;

	private volatile MessageChannel afterRollbackResultChannel;

	public void setBeforeCommitResultChannel(MessageChannel beforeCommitResultChannel) {
		Assert.notNull(beforeCommitResultChannel, "'beforeCommitResultChannel' must not be null");
		this.beforeCommitResultChannel = beforeCommitResultChannel;
	}

	public void setAfterCommitResultChannel(MessageChannel afterCommitResultChannel) {
		Assert.notNull(afterCommitResultChannel, "'afterCommitResultChannel' must not be null");
		this.afterCommitResultChannel = afterCommitResultChannel;
	}

	public void setAfterRollbackResultChannel(MessageChannel afterRollbackResultChannel) {
		Assert.notNull(afterRollbackResultChannel, "'afterRollbackResultChannel' must not be null");
		this.afterRollbackResultChannel = afterRollbackResultChannel;
	}

	public void setBeforeCommitExpression(Expression beforeCommitExpression) {
		Assert.notNull(beforeCommitExpression, "'beforeCommitExpression' must not be null");
		this.beforeCommitExpression = beforeCommitExpression;
	}

	public void setAfterCommitExpression(Expression afterCommitExpression) {
		Assert.notNull(afterCommitExpression, "'afterCommitExpression' must not be null");
		this.afterCommitExpression = afterCommitExpression;
	}

	public void setAfterRollbackExpression(Expression afterRollbackExpression) {
		Assert.notNull(afterRollbackExpression, "'afterRollbackExpression' must not be null");
		this.afterRollbackExpression = afterRollbackExpression;
	}

	public void processBeforeCommit(Message<?> message, Object resource){
		this.doProcess(message, resource, this.beforeCommitExpression, this.beforeCommitResultChannel, "beforeCommit");
	}

	public void processAfterCommit(Message<?> message, Object resource){
		this.doProcess(message, resource, this.afterCommitExpression, this.afterCommitResultChannel, "afterCommit");
	}

	public void processAfterRollback(Message<?> message, Object resource){
		this.doProcess(message, resource, this.afterRollbackExpression, this.afterRollbackResultChannel, "afterRollback");
	}

	private void doProcess(Message<?> message, Object resource, Expression expression, MessageChannel messageChannel, String expressionType) {
		if (message != null){
			if (expression != null){
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
					Message<?> spelResultMessage = null;
					if (logger.isDebugEnabled()) {
						logger.debug("Sending expression result message to " + messageChannel + " " +
								"as part of '" + expressionType + "' transaction synchronization");
					}
					try {
						spelResultMessage = MessageBuilder.withPayload(value).build();
						messageChannel.send(spelResultMessage, 0);
					}
					catch (Exception e) {
						logger.error("Failed to send " + expressionType + " evaluation result " + spelResultMessage, e);
					}
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Sending received message to " + messageChannel + " as part of '" +
							expressionType + "' transaction synchronization");
				}
				try {
					messageChannel.send(MessageBuilder.fromMessage(message).build());
				} catch (Exception e) {
					logger.error("Failed to send " + message, e);
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
		if (resource != TransactionSynchronizationFactory.NO_TX_RESOURCE) {
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
}
