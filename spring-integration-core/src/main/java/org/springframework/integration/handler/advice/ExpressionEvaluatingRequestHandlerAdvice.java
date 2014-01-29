/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.handler.advice;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.message.AdviceMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * Used to advise {@link MessageHandler}s.
 * Two expressions 'onSuccessExpression' and 'onFailureExpression' are evaluated when
 * appropriate. If the evaluation returns a result, a message is sent to the onSuccessChannel
 * or onFailureChannel as appropriate; the message is the input message with a header
 * {@link org.springframework.integration.IntegrationMessageHeaderAccessor#POSTPROCESS_RESULT} containing the evaluation result.
 * The failure expression is NOT evaluated if the success expression throws an exception.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class ExpressionEvaluatingRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	private volatile Expression  onSuccessExpression;

	private volatile MessageChannel successChannel;

	private volatile Expression onFailureExpression;

	private volatile MessageChannel failureChannel;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile boolean trapException = false;

	private volatile boolean returnFailureExpressionResult = false;

	private volatile boolean propagateOnSuccessEvaluationFailures;

	private volatile EvaluationContext evaluationContext;

	public void setOnSuccessExpression(String onSuccessExpression) {
		Assert.notNull(onSuccessExpression, "'onSuccessExpression' must not be null");
		this.onSuccessExpression = new SpelExpressionParser().parseExpression(onSuccessExpression);
	}

	public void setOnFailureExpression(String onFailureExpression) {
		Assert.notNull(onFailureExpression, "'onFailureExpression' must not be null");
		this.onFailureExpression = new SpelExpressionParser().parseExpression(onFailureExpression);
	}

	public void setSuccessChannel(MessageChannel successChannel) {
		Assert.notNull(successChannel,"'successChannel' must not be null");
		this.successChannel = successChannel;
	}

	public void setFailureChannel(MessageChannel failureChannel) {
		Assert.notNull(failureChannel,"'failureChannel' must not be null");
		this.failureChannel = failureChannel;
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
	 *
	 * @param returnFailureExpressionResult true to return the result of the evaluation.
	 */
	public void setReturnFailureExpressionResult(boolean returnFailureExpressionResult) {
		this.returnFailureExpressionResult = returnFailureExpressionResult;
	}

	/**
	 * If true and an onSuccess expression evaluation fails with an exception, the exception will be thrown to the
	 * caller. If false, the exception is caught. Default false. Ignored for onFailure expression evaluation - the
	 * original exception will be propagated (unless trapException is true).
	 * @param propagateOnSuccessEvaluationFailures The propagateOnSuccessEvaluationFailures to set.
	 */
	public void setPropagateEvaluationFailures(boolean propagateOnSuccessEvaluationFailures) {
		this.propagateOnSuccessEvaluationFailures = propagateOnSuccessEvaluationFailures;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(this.getBeanFactory());
		}
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
		try {
			Object result = callback.execute();
			if (this.onSuccessExpression != null) {
				this.evaluateSuccessExpression(message);
			}
			return result;
		}
		catch (Exception e) {
			Exception actualException = this.unwrapExceptionIfNecessary(e);
			if (this.onFailureExpression != null) {
				Object evalResult = this.evaluateFailureExpression(message, actualException);
				if (this.returnFailureExpressionResult) {
					return evalResult;
				}
			}
			if (!this.trapException) {
				throw actualException;
			}
			return null;
		}
	}

	private void evaluateSuccessExpression(Message<?> message) throws Exception {
		Object evalResult;
		boolean evaluationFailed = false;
		try {
			evalResult = this.onSuccessExpression.getValue(this.prepareEvaluationContextToUse(null), message);
		}
		catch (Exception e) {
			evalResult = e;
			evaluationFailed = true;
		}
		if (evalResult != null && this.successChannel != null) {
			AdviceMessage resultMessage = new AdviceMessage(evalResult, message);
			this.messagingTemplate.send(this.successChannel, resultMessage);
		}
		if (evaluationFailed && this.propagateOnSuccessEvaluationFailures) {
			throw (Exception) evalResult;
		}
	}

	private Object evaluateFailureExpression(Message<?> message, Exception exception) throws Exception {
		Object evalResult;
		try {
			evalResult = this.onFailureExpression.getValue(this.prepareEvaluationContextToUse(exception), message);
		}
		catch (Exception e) {
			evalResult = e;
			logger.error("Failure expression evaluation failed for " + message + ": " + e.getMessage());
		}
		if (evalResult != null && this.failureChannel != null) {
			MessagingException messagingException = new MessageHandlingExpressionEvaluatingAdviceException(message,
					"Handler Failed", this.unwrapThrowableIfNecessary(exception), evalResult);
			ErrorMessage resultMessage = new ErrorMessage(messagingException);
			this.messagingTemplate.send(this.failureChannel, resultMessage);
		}
		return evalResult;
	}

	protected StandardEvaluationContext createEvaluationContext(){
		return ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
	}

	/**
	 * If we don't need variables (i.e., exception is null)
	 * we can use a singleton context; otherwise we need a new one each time.
	 * @param exception
	 * @return The context.
	 */
	private EvaluationContext prepareEvaluationContextToUse(Exception exception) {
		EvaluationContext evaluationContextToUse;
		if (exception != null) {
			evaluationContextToUse = this.createEvaluationContext();
			evaluationContextToUse.setVariable("exception", exception);
		}
		else {
			if (this.evaluationContext == null) {
				this.evaluationContext = this.createEvaluationContext();
			}
			evaluationContextToUse = this.evaluationContext;
		}
		return evaluationContextToUse;
	}

	public static class MessageHandlingExpressionEvaluatingAdviceException extends MessagingException {

		private static final long serialVersionUID = 1L;

		private final Object evaluationResult;

		public MessageHandlingExpressionEvaluatingAdviceException(Message<?> message, String description,
				Throwable cause, Object evaluationResult) {
			super(message, description, cause);
			this.evaluationResult = evaluationResult;
		}

		public Object getEvaluationResult() {
			return evaluationResult;
		}

	}

}
