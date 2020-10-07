/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.transaction;

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * This implementation of {@link TransactionSynchronizationFactory}
 * allows you to configure SpEL expressions, with their execution being coordinated
 * (synchronized) with a transaction - see
 * {@link org.springframework.transaction.support.TransactionSynchronization}.
 * Expressions for {@code before-commit}, {@code after-commit}, and {@code after-rollback}
 * are supported, together with a {@code channel} for each where the evaluation result
 * (if any) will be sent.
 * <p>
 * For each sub-element you can specify 'expression' and/or 'channel' attributes.
 * If only the 'channel' attribute is present the received Message will be sent
 * there as part of a particular synchronization scenario.
 * <p>
 * If only the 'expression' attribute is present and the result of an expression
 * is a non-Null value, a Message with the result as the payload will be generated
 * and sent to a default channel (NullChannel) and will appear in the logs.
 * If you want the evaluation result to go to a specific channel
 * add a 'channel' attribute. If the result of an expression is null or void,
 * no Message will be generated.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class ExpressionEvaluatingTransactionSynchronizationProcessor extends IntegrationObjectSupport
		implements TransactionSynchronizationProcessor {

	private volatile EvaluationContext evaluationContext;

	private volatile Expression beforeCommitExpression;

	private volatile Expression afterCommitExpression;

	private volatile Expression afterRollbackExpression;

	private volatile MessageChannel beforeCommitChannel;

	private volatile MessageChannel afterCommitChannel;

	private volatile MessageChannel afterRollbackChannel;

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	public void setBeforeCommitChannel(MessageChannel beforeCommitChannel) {
		Assert.notNull(beforeCommitChannel, "'beforeCommitChannel' must not be null");
		this.beforeCommitChannel = beforeCommitChannel;
	}

	public void setAfterCommitChannel(MessageChannel afterCommitChannel) {
		Assert.notNull(afterCommitChannel, "'afterCommitChannel' must not be null");
		this.afterCommitChannel = afterCommitChannel;
	}

	public void setAfterRollbackChannel(MessageChannel afterRollbackChannel) {
		Assert.notNull(afterRollbackChannel, "'afterRollbackChannel' must not be null");
		this.afterRollbackChannel = afterRollbackChannel;
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

	@Override
	protected void onInit() {
		super.onInit();
		if (this.evaluationContext == null) {
			this.evaluationContext = createEvaluationContext();
		}
	}

	@Override
	public void processBeforeCommit(IntegrationResourceHolder holder) {
		doProcess(holder, this.beforeCommitExpression, this.beforeCommitChannel, "beforeCommit");
	}

	@Override
	public void processAfterCommit(IntegrationResourceHolder holder) {
		doProcess(holder, this.afterCommitExpression, this.afterCommitChannel, "afterCommit");
	}

	@Override
	public void processAfterRollback(IntegrationResourceHolder holder) {
		doProcess(holder, this.afterRollbackExpression, this.afterRollbackChannel, "afterRollback");
	}

	private void doProcess(IntegrationResourceHolder holder, Expression expression,
			@Nullable MessageChannel messageChannel, String expressionType) {

		Message<?> message = holder.getMessage();
		if (message != null) {
			if (expression != null) {
				logger.debug(() -> "Evaluating " + expressionType + " expression: '" + expression.getExpressionString()
						+ "' on " + message);
				EvaluationContext evaluationContextToUse = prepareEvaluationContextToUse(holder);
				Object value = expression.getValue(evaluationContextToUse, message);
				if (value != null && messageChannel != null) {
					sendMessageForExpressionResult(value, message.getHeaders(), messageChannel, expressionType);
				}
				else {
					logger.trace("Expression evaluation returned null");
				}
			}
			else if (messageChannel != null) {
				logger.debug(() -> "Sending received message to " + messageChannel + " as part of '" +
						expressionType + "' transaction synchronization");
				try {
					// rollback will be initiated if any of the previous sync operations fail (e.g., beforeCommit)
					// this means that this method will be called without explicit configuration thus no channel
					sendMessage(messageChannel, message);
				}
				catch (Exception ex) {
					logger.error(ex, () -> "Failed to send " + message);
				}

			}
		}
	}

	private void sendMessageForExpressionResult(Object value, Map<String, ?> headers,
			MessageChannel messageChannel, String expressionType) {

		logger.debug(() -> "Sending expression result message to " + messageChannel + " " +
				"as part of '" + expressionType + "' transaction synchronization");
		Message<?> spelResultMessage;
			if (value instanceof Message<?>) {
				spelResultMessage = (Message<?>) value;
			}
			else {
				spelResultMessage =
						getMessageBuilderFactory()
								.withPayload(value)
								.copyHeaders(headers)
								.build();
			}
		try {
			sendMessage(messageChannel, spelResultMessage);
		}
		catch (Exception ex) {
			logger.error(ex, () -> "Failed to send " + expressionType + " evaluation result " + spelResultMessage);
		}
	}

	private void sendMessage(MessageChannel channel, Message<?> message) {
		channel.send(message, 0);
	}

	/**
	 * If we don't need variables (i.e., resource is null)
	 * we can use a singleton context; otherwise we need a new one each time.
	 * @param resource The resource
	 * @return The context.
	 */
	private EvaluationContext prepareEvaluationContextToUse(Object resource) {
		if (resource != null) {
			EvaluationContext evaluationContextWithVariables = createEvaluationContext();
			if (resource instanceof IntegrationResourceHolder) {
				IntegrationResourceHolder holder = (IntegrationResourceHolder) resource;
				for (Entry<String, Object> entry : holder.getAttributes().entrySet()) {
					String key = entry.getKey();
					evaluationContextWithVariables.setVariable(key, entry.getValue());
				}
			}
			return evaluationContextWithVariables;
		}
		else {
			return this.evaluationContext;
		}
	}

	protected StandardEvaluationContext createEvaluationContext() {
		return ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

}
