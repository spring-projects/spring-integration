/*
 * Copyright 2002-2014 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.dispatcher.AggregateMessageDeliveryException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MessageHandler implementation that simply logs the Message or its payload depending on the value of the
 * 'shouldLogFullMessage' property. If logging the payload, and it is assignable to Throwable, it will log the stack
 * trace. By default, it will log the payload only.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 1.0.1
 */
public class LoggingHandler extends AbstractMessageHandler {

	public static enum Level {
		FATAL, ERROR, WARN, INFO, DEBUG, TRACE
	}

	private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();


	private volatile Expression expression;

	private volatile boolean expressionSet;

	private volatile boolean shouldLogFullMessageSet;

	private volatile Level level;

	private volatile EvaluationContext evaluationContext;

	private volatile Log messageLogger = this.logger;


	/**
	 * Create a LoggingHandler with the given log level (case-insensitive).
	 * <p>
	 * The valid levels are: FATAL, ERROR, WARN, INFO, DEBUG, or TRACE
	 * </p>
	 * @param level The level.
	 */
	public LoggingHandler(String level) {
		Assert.notNull(level, "'level' cannot be null");
		try {
			this.level = Level.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level
					+ "'. The (case-insensitive) supported values are: "
					+ StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext();
		this.expression = EXPRESSION_PARSER.parseExpression("payload");
	}

	public void setExpression(String expressionString) {
		Assert.isTrue(!(this.shouldLogFullMessageSet), "Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		this.expressionSet = true;
		this.expression = EXPRESSION_PARSER.parseExpression(expressionString);
	}

	/**
	 * @return The current logging {@link Level}.
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Set the logging {@link Level}.
	 *
	 * @param level the level.
	 */
	public void setLevel(Level level) {
		Assert.notNull(level, "'level' cannot be null");
		this.level = level;
	}

	public void setLoggerName(String loggerName) {
		Assert.hasText(loggerName, "loggerName must not be empty");
		this.messageLogger = LogFactory.getLog(loggerName);
	}

	/**
	 * Specify whether to log the full Message. Otherwise, only the payload will be logged. This value is
	 * <code>false</code> by default.
	 *
	 * @param shouldLogFullMessage true if the complete message should be logged.
	 */
	public void setShouldLogFullMessage(boolean shouldLogFullMessage) {
		Assert.isTrue(!(this.expressionSet), "Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		this.shouldLogFullMessageSet = true;
		this.expression = (shouldLogFullMessage) ? EXPRESSION_PARSER.parseExpression("#root") : EXPRESSION_PARSER
				.parseExpression("payload");
	}

	@Override
	public String getComponentType() {
		return "logging-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.getBeanFactory());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		switch (this.level) {
		case FATAL:
			if (messageLogger.isFatalEnabled()) {
				messageLogger.fatal(createLogMessage(message));
			}
			break;
		case ERROR:
			if (messageLogger.isErrorEnabled()) {
				messageLogger.error(createLogMessage(message));
			}
			break;
		case WARN:
			if (messageLogger.isWarnEnabled()) {
				messageLogger.warn(createLogMessage(message));
			}
			break;
		case INFO:
			if (messageLogger.isInfoEnabled()) {
				messageLogger.info(createLogMessage(message));
			}
			break;
		case DEBUG:
			if (messageLogger.isDebugEnabled()) {
				messageLogger.debug(createLogMessage(message));
			}
			break;
		case TRACE:
			if (messageLogger.isTraceEnabled()) {
				messageLogger.trace(createLogMessage(message));
			}
			break;
		default:
			throw new IllegalStateException("Level '" + this.level + "' is not supported");
		}
	}


	private Object createLogMessage(Message<?> message) {
		Object logMessage = this.expression.getValue(this.evaluationContext, message);
		if (logMessage instanceof Throwable) {
			StringWriter stringWriter = new StringWriter();
			if (logMessage instanceof AggregateMessageDeliveryException) {
				stringWriter.append(((Throwable) logMessage).getMessage());
				for (Exception exception : ((AggregateMessageDeliveryException)logMessage).getAggregatedExceptions()) {
					exception.printStackTrace(new PrintWriter(stringWriter, true));
				}
			}
			else {
				((Throwable) logMessage).printStackTrace(new PrintWriter(stringWriter, true));
			}
			logMessage = stringWriter.toString();
		}
		return logMessage;
	}

}
