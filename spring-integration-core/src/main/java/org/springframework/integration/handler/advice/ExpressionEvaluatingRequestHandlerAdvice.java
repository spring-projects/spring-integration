/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.handler.advice;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.StringUtils;

/**
 * Used to advise {@link org.springframework.messaging.MessageHandler}s.
 * Two expressions 'onSuccessExpression' and 'onFailureExpression' are evaluated when
 * appropriate. If the evaluation returns a result, a message is sent to the onSuccessChannel
 * or onFailureChannel as appropriate; the message is an {@link AdviceMessage}
 * containing the evaluation result in its payload and the {@code inputMessage} property containing
 * the original message that was sent to the endpoint.
 * The failure expression is NOT evaluated if the success expression throws an exception.
 * <p>
 * When expressions are not configured, but channels are, the default expression is evaluated
 * just into a {@code payload} from the message.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class ExpressionEvaluatingRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	private static final Expression DEFAULT_EXPRESSION = new FunctionExpression<Message<?>>(Message::getPayload);

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private Expression onSuccessExpression;

	private MessageChannel successChannel;

	private String successChannelName;

	private Expression onFailureExpression;

	private MessageChannel failureChannel;

	private String failureChannelName;

	private boolean trapException = false;

	private boolean returnFailureExpressionResult = false;

	private boolean propagateOnSuccessEvaluationFailures;

	private EvaluationContext evaluationContext;

	/**
	 * Set the expression to evaluate against the message after a successful handler invocation.
	 * Defaults to {@code payload}, if {@code successChannel} is configured.
	 * @param onSuccessExpression the SpEL expression.
	 * @since 4.3.7
	 */
	public void setOnSuccessExpressionString(String onSuccessExpression) {
		setOnSuccessExpression(EXPRESSION_PARSER.parseExpression(onSuccessExpression));
	}

	/**
	 * Set the expression to evaluate against the message after a successful handler invocation.
	 * Defaults to {@code payload}, if {@code successChannel} is configured.
	 * @param onSuccessExpression the SpEL expression.
	 * @since 5.0
	 */
	public void setOnSuccessExpression(@Nullable Expression onSuccessExpression) {
		this.onSuccessExpression = onSuccessExpression;
	}

	/**
	 * Set the expression to evaluate against the root message after a failed
	 * handler invocation. The exception is available as the variable {@code #exception}.
	 * Defaults to {@code payload}, if {@code failureChannel} is configured.
	 * @param onFailureExpression the SpEL expression.
	 * @since 4.3.7
	 */
	public void setOnFailureExpressionString(String onFailureExpression) {
		setOnFailureExpression(EXPRESSION_PARSER.parseExpression(onFailureExpression));
	}

	/**
	 * Set the expression to evaluate against the root message after a failed
	 * handler invocation. The exception is available as the variable {@code #exception}.
	 * Defaults to {@code payload}, if {@code failureChannel} is configured.
	 * @param onFailureExpression the SpEL expression.
	 * @since 5.0
	 */
	public void setOnFailureExpression(@Nullable Expression onFailureExpression) {
		this.onFailureExpression = onFailureExpression;
	}

	/**
	 * Set the channel to which to send the {@link AdviceMessage} after evaluating the
	 * success expression.
	 * @param successChannel the channel.
	 */
	public void setSuccessChannel(MessageChannel successChannel) {
		this.successChannel = successChannel;
	}

	/**
	 * Set the channel name to which to send the {@link AdviceMessage} after evaluating
	 * the success expression.
	 * @param successChannelName the channel name.
	 * @since 4.3.7
	 */
	public void setSuccessChannelName(String successChannelName) {
		this.successChannelName = successChannelName;
	}

	/**
	 * Set the channel to which to send the {@link ErrorMessage} after evaluating the
	 * failure expression.
	 * @param failureChannel the channel.
	 */
	public void setFailureChannel(MessageChannel failureChannel) {
		this.failureChannel = failureChannel;
	}

	/**
	 * Set the channel name to which to send the {@link ErrorMessage} after evaluating the
	 * failure expression.
	 * @param failureChannelName the channel name.
	 * @since 4.3.7
	 */
	public void setFailureChannelName(String failureChannelName) {
		this.failureChannelName = failureChannelName;
	}

	/**
	 * If true, any exception will be caught and null returned.
	 * Default false.
	 * @param trapException true to trap Exceptions.
	 */
	public void setTrapException(boolean trapException) {
		this.trapException = trapException;
	}

	/**
	 * If true, the result of evaluating the onFailureExpression will
	 * be returned as the result of AbstractReplyProducingMessageHandler.handleRequestMessage(Message).
	 * @param returnFailureExpressionResult true to return the result of the evaluation.
	 */
	public void setReturnFailureExpressionResult(boolean returnFailureExpressionResult) {
		this.returnFailureExpressionResult = returnFailureExpressionResult;
	}

	/**
	 * If true and an onSuccess expression evaluation fails with an exception, the
	 * exception will be thrown to the caller. If false, the exception is caught. Default
	 * false. Ignored for onFailure expression evaluation - the original exception will be
	 * propagated (unless trapException is true).
	 * @param propagateOnSuccessEvaluationFailures The
	 * propagateOnSuccessEvaluationFailures to set.
	 */
	public void setPropagateEvaluationFailures(boolean propagateOnSuccessEvaluationFailures) {
		this.propagateOnSuccessEvaluationFailures = propagateOnSuccessEvaluationFailures;
	}

	@Override
	protected void onInit() {
		super.onInit();
		BeanFactory beanFactory = getBeanFactory();
		if (beanFactory != null) {
			this.messagingTemplate.setBeanFactory(beanFactory);
		}

		if (this.onSuccessExpression == null
				&& (this.successChannel != null || StringUtils.hasText(this.successChannelName))) {

			this.onSuccessExpression = DEFAULT_EXPRESSION;
		}

		if (this.onFailureExpression == null
				&& (this.failureChannel != null || StringUtils.hasText(this.failureChannelName))) {

			this.onFailureExpression = DEFAULT_EXPRESSION;
		}
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		try {
			Object result = callback.execute();
			if (this.onSuccessExpression != null) {
				evaluateSuccessExpression(message);
			}
			return result;
		}
		catch (RuntimeException e) {
			Exception actualException = unwrapExceptionIfNecessary(e);
			if (this.onFailureExpression != null) {
				Object evalResult = evaluateFailureExpression(message, actualException);
				if (this.returnFailureExpressionResult) {
					return evalResult;
				}
			}
			if (!this.trapException) {
				if (e instanceof ThrowableHolderException) { // NOSONAR
					throw e;
				}
				else {
					throw new ThrowableHolderException(actualException); // NOSONAR lost stack trace
				}
			}
			return null;
		}
	}

	private void evaluateSuccessExpression(Message<?> message) {
		Object evalResult;
		try {
			evalResult = this.onSuccessExpression.getValue(prepareEvaluationContextToUse(null), message);
		}
		catch (Exception e) {
			evalResult = e;
		}
		DestinationResolver<MessageChannel> channelResolver = getChannelResolver();
		if (this.successChannel == null && this.successChannelName != null && channelResolver != null) {
			this.successChannel = channelResolver.resolveDestination(this.successChannelName);
		}
		if (evalResult != null && this.successChannel != null) {
			AdviceMessage<?> resultMessage = new AdviceMessage<>(evalResult, message);
			this.messagingTemplate.send(this.successChannel, resultMessage);
		}
		if (evalResult instanceof Exception && this.propagateOnSuccessEvaluationFailures) {
			throw new ThrowableHolderException((Exception) evalResult);
		}
	}

	private Object evaluateFailureExpression(Message<?> message, Exception exception) {
		Object evalResult;
		try {
			evalResult = this.onFailureExpression.getValue(prepareEvaluationContextToUse(exception), message);
		}
		catch (Exception e) {
			evalResult = e;
			logger.error("Failure expression evaluation failed for " + message + ": " + e.getMessage());
		}
		DestinationResolver<MessageChannel> channelResolver = getChannelResolver();
		if (this.failureChannel == null && this.failureChannelName != null && channelResolver != null) {
			this.failureChannel = channelResolver.resolveDestination(this.failureChannelName);
		}
		if (evalResult != null && this.failureChannel != null) {
			MessagingException messagingException =
					new MessageHandlingExpressionEvaluatingAdviceException(message, "Handler Failed",
							unwrapThrowableIfNecessary(exception), evalResult);
			ErrorMessage errorMessage = new ErrorMessage(messagingException, message.getHeaders());
			this.messagingTemplate.send(this.failureChannel, errorMessage);
		}
		return evalResult;
	}

	protected StandardEvaluationContext createEvaluationContext() {
		return ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	/**
	 * If we don't need variables (i.e., exception is null)
	 * we can use a singleton context; otherwise we need a new one each time.
	 * @param exception the {@link Exception} to use in the context.
	 * @return The context.
	 */
	private EvaluationContext prepareEvaluationContextToUse(Exception exception) {
		EvaluationContext evaluationContextToUse;
		if (exception != null) {
			evaluationContextToUse = createEvaluationContext();
			evaluationContextToUse.setVariable("exception", exception);
		}
		else {
			if (this.evaluationContext == null) {
				this.evaluationContext = createEvaluationContext();
			}
			evaluationContextToUse = this.evaluationContext;
		}
		return evaluationContextToUse;
	}

	public static class MessageHandlingExpressionEvaluatingAdviceException extends MessagingException {

		private static final long serialVersionUID = 1L;

		private final transient Object evaluationResult;

		public MessageHandlingExpressionEvaluatingAdviceException(Message<?> message, String description,
				Throwable cause, Object evaluationResult) {

			super(message, description, cause);
			this.evaluationResult = evaluationResult;
		}

		public Object getEvaluationResult() {
			return this.evaluationResult;
		}

	}

}
