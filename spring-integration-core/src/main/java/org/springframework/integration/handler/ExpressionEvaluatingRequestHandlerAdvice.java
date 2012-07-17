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
package org.springframework.integration.handler;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;

/**
 * Used to advise {@link AbstractReplyProducingMessageHandler#handleRequestMessage(Message)}.
 * Two expressions 'onSuccessExpression' and 'onFailureExpression' are evaluated when
 * appropriate. If the evaluation returns a result, a message is sent to the onSuccessChannel
 * or onFailureChannel as appropriate; the message is the input message with a header
 * {@link MessageHeaders#POSTPROCESS_RESULT} containing the evaluation result.
 * The failure expression is NOT evaluated if the success expression throws an exception.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class ExpressionEvaluatingRequestHandlerAdvice extends AbstractRequestHandlerAdvice
		implements BeanFactoryAware {

	private final ExpressionEvaluatingMessageProcessor<Object> onSuccessMessageProcessor;

	private final MessageChannel successChannel;

	private final ExpressionEvaluatingMessageProcessor<Object> onFailureMessageProcessor;

	private final MessageChannel failureChannel;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile boolean trapException = false;

	private volatile boolean returnFailureExpressionResult = false;

	private volatile BeanFactory beanFactory;

	private volatile boolean propagateOnSuccessEvaluationFailures;

	/**
	 * @param onSuccessExpression
	 * @param successChannel
	 * @param onFailureExpression
	 * @param failureChannel
	 */
	public ExpressionEvaluatingRequestHandlerAdvice(Expression onSuccessExpression, MessageChannel successChannel,
			Expression onFailureExpression, MessageChannel failureChannel) {
		if (onSuccessExpression != null) {
			this.onSuccessMessageProcessor = new ExpressionEvaluatingMessageProcessor<Object>(onSuccessExpression);
			this.onSuccessMessageProcessor.setBeanFactory(this.beanFactory);
		}
		else {
			this.onSuccessMessageProcessor = null;
		}
		this.successChannel = successChannel;
		if (onFailureExpression != null) {
			this.onFailureMessageProcessor = new ExpressionEvaluatingMessageProcessor<Object>(onFailureExpression);
			onFailureMessageProcessor.setBeanFactory(this.beanFactory);
		}
		else {
			this.onFailureMessageProcessor = null;
		}
		this.failureChannel = failureChannel;

	}

	/**
	 * If true, any exception will be caught and null returned.
	 * Default false.
	 * @param trapException
	 */
	public void setTrapException(boolean trapException) {
		this.trapException = trapException;
	}

	/**
	 * If true, the result of evaluating the onFailureExpression will
	 * be returned as the result of {@link AbstractReplyProducingMessageHandler#handleRequestMessage(Message)}.
	 * @param returnFailureExpressionResult
	 */
	public void setReturnFailureExpressionResult(boolean returnFailureExpressionResult) {
		this.returnFailureExpressionResult = returnFailureExpressionResult;
	}

	/**
	 * If true and an onSuccess expression evaluation fails with an exception, the exception will be thrown to the
	 * caller. If false, the exception is caught. Default false. Ignored for onFailure expression evaluation - the
	 * original exception will be propagated (unless trapException is true).
	 * @param propagateOnSuccessEvaluationFailures
	 */
	public void setPropagateEvaluationFailures(boolean propagateOnSuccessEvaluationFailures) {
		this.propagateOnSuccessEvaluationFailures = propagateOnSuccessEvaluationFailures;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	protected Object doInvoke(MethodInvocation invocation) throws Throwable {
		Message<?> message = (Message<?>) invocation.getArguments()[0];
		try {
			Object result = invocation.proceed();
			if (onSuccessMessageProcessor != null) {
				evaluateExpression(message, this.onSuccessMessageProcessor, this.successChannel, this.propagateOnSuccessEvaluationFailures);
			}
			return result;
		}
		catch (Throwable t) {
			Object evalResult = evaluateExpression(message, this.onFailureMessageProcessor, this.failureChannel, false);
			if (this.returnFailureExpressionResult) {
				return evalResult;
			}
			if (!this.trapException) {
				throw t;
			}
			return null;
		}
	}

	private Object evaluateExpression(Message<?> message,
			ExpressionEvaluatingMessageProcessor<Object> expressionEvaluatingMessageProcessor,
			MessageChannel resultChannel, boolean propagateEvaluationFailure) throws Exception {
		Object evalResult;
		boolean evaluationFailed = false;
		try {
			evalResult = expressionEvaluatingMessageProcessor.processMessage(message);
		}
		catch (Exception e) {
			evalResult = e;
			evaluationFailed = true;
		}
		if (evalResult != null && resultChannel != null) {
			message = MessageBuilder.fromMessage(message)
					.setHeader(MessageHeaders.POSTPROCESS_RESULT, evalResult)
					.build();
			this.messagingTemplate.send(resultChannel, message);
		}
		if (evaluationFailed && propagateEvaluationFailure) {
			throw (Exception) evalResult;
		}
		return evalResult;
	}

}
