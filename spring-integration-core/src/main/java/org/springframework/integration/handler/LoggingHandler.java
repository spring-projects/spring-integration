/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.Message;
import org.springframework.integration.dispatcher.AggregateMessageDeliveryException;
import org.springframework.util.StringUtils;

/**
 * MessageHandler implementation that simply logs the Message or its payload depending on the value of the
 * 'shouldLogFullMessage' property. If logging the payload, and it is assignable to Throwable, it will log the stack
 * trace. By default, it will log the payload only.
 * 
 * @author Mark Fisher
 * @since 1.0.1
 */
public class LoggingHandler extends AbstractMessageHandler {

	private static enum Level {
		FATAL, ERROR, WARN, INFO, DEBUG, TRACE
	}

	private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	private volatile Expression expression;

	private final Level level;

	private final EvaluationContext evaluationContext;

	/**
	 * Create a LoggingHandler with the given log level (case-insensitive).
	 * <p>
	 * The valid levels are: FATAL, ERROR, WARN, INFO, DEBUG, or TRACE
	 */
	public LoggingHandler(String level) {
		try {
			this.level = Level.valueOf(level.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level
					+ "'. The (case-insensitive) supported values are: "
					+ StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		this.evaluationContext = evaluationContext;
		this.expression = EXPRESSION_PARSER.parseExpression("payload");
	}
	
	public void setExpression(String expressionString) {
		this.expression = EXPRESSION_PARSER.parseExpression(expressionString);
	}

	/**
	 * Specify whether to log the full Message. Otherwise, only the payload will be logged. This value is
	 * <code>false</code> by default.
	 */
	public void setShouldLogFullMessage(boolean shouldLogFullMessage) {
		this.expression = (shouldLogFullMessage) ? EXPRESSION_PARSER.parseExpression("#root") : EXPRESSION_PARSER
				.parseExpression("payload");
	}

	@Override
	public String getComponentType() {
		return "logging-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object logMessage = this.expression.getValue(this.evaluationContext, message);
		if (logMessage instanceof Throwable) {
			StringWriter stringWriter = new StringWriter();
			if (logMessage instanceof AggregateMessageDeliveryException) {
				stringWriter.append(((Throwable) logMessage).getMessage());
				for (Exception exception : (List<? extends Exception>) ((AggregateMessageDeliveryException)logMessage).getAggregatedExceptions()) {
					exception.printStackTrace(new PrintWriter(stringWriter, true));
				}
			} else {
				((Throwable) logMessage).printStackTrace(new PrintWriter(stringWriter, true));
			}
			logMessage = stringWriter.toString();
		}
		switch (this.level) {
		case FATAL:
			if (logger.isFatalEnabled()) {
				logger.fatal(logMessage);
			}
			break;
		case ERROR:
			if (logger.isErrorEnabled()) {
				logger.error(logMessage);
			}
			break;
		case WARN:
			if (logger.isWarnEnabled()) {
				logger.warn(logMessage);
			}
			break;
		case INFO:
			if (logger.isInfoEnabled()) {
				logger.info(logMessage);
			}
			break;
		case DEBUG:
			if (logger.isDebugEnabled()) {
				logger.debug(logMessage);
			}
			break;
		case TRACE:
			if (logger.isTraceEnabled()) {
				logger.trace(logMessage);
			}
			break;
		}
	}

}
