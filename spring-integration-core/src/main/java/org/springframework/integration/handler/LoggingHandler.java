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

package org.springframework.integration.handler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.core.log.LogAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.dispatcher.AggregateMessageDeliveryException;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * MessageHandler implementation that simply logs the Message or its payload depending on the value of the
 * 'shouldLogFullMessage' or SpEL 'logExpression' property.
 * If logging the payload, and it is assignable to Throwable, it will log the stack
 * trace. By default, it will log the payload only.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Andriy Kryvtsun
 *
 * @since 1.0.1
 */
public class LoggingHandler extends AbstractMessageHandler {

	public enum Level {

		FATAL, ERROR, WARN, INFO, DEBUG, TRACE

	}

	private Level level;

	private Expression expression = new FunctionExpression<Message<?>>(Message::getPayload);

	private boolean expressionSet;

	private EvaluationContext evaluationContext = ExpressionUtils.createStandardEvaluationContext();

	private boolean shouldLogFullMessageSet;

	private LogAccessor messageLogger = this.logger;

	/**
	 * Create a LoggingHandler with the given log level (case-insensitive).
	 * <p>
	 * The valid levels are: FATAL, ERROR, WARN, INFO, DEBUG, or TRACE
	 * </p>
	 * @param level The level.
	 * @see #LoggingHandler(Level)
	 */
	public LoggingHandler(String level) {
		this(convertLevel(level));
	}

	private static Level convertLevel(String level) {
		Assert.hasText(level, "'level' cannot be empty");
		try {
			return Level.valueOf(level.toUpperCase());
		}
		catch (@SuppressWarnings("unused") IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level // NOSONAR lost stack trace
					+ "'. The (case-insensitive) supported values are: "
					+ StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
	}

	/**
	 * Create a {@link LoggingHandler} with the given log {@link Level}.
	 * @param level the {@link Level} to use.
	 * @since 4.3
	 */
	public LoggingHandler(Level level) {
		doSetLevel(level);
	}

	/**
	 * Set a SpEL expression string to use.
	 * @param expressionString the SpEL expression string to use.
	 * @since 4.3
	 * @see #setLogExpression(Expression)
	 */
	public void setLogExpressionString(String expressionString) {
		Assert.hasText(expressionString, "'expressionString' must not be empty");
		setLogExpression(EXPRESSION_PARSER.parseExpression(expressionString));
	}

	/**
	 * Set an {@link Expression} to evaluate a log entry at runtime against the request {@link Message}.
	 * @param expression the {@link Expression} to use.
	 * @since 4.3
	 * @see #setLogExpressionString(String)
	 */
	public void setLogExpression(Expression expression) {
		Assert.isTrue(!(this.shouldLogFullMessageSet),
				"Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		this.expressionSet = true;
		this.expression = expression;
	}

	/**
	 * @return The current logging {@link Level}.
	 */
	public Level getLevel() {
		return this.level;
	}

	/**
	 * Set the logging {@link Level} to change the behavior at runtime.
	 * @param level the level.
	 */
	public void setLevel(Level level) {
		doSetLevel(level);
	}

	private void doSetLevel(Level level) {
		Assert.notNull(level, "'level' cannot be null");
		this.level = level;
	}

	public void setLoggerName(String loggerName) {
		Assert.hasText(loggerName, "loggerName must not be empty");
		this.messageLogger = new LogAccessor(loggerName);
	}

	/**
	 * Specify whether to log the full Message. Otherwise, only the payload will be logged. This value is
	 * <code>false</code> by default.
	 * @param shouldLogFullMessage true if the complete message should be logged.
	 */
	public void setShouldLogFullMessage(boolean shouldLogFullMessage) {
		Assert.isTrue(!(this.expressionSet), "Cannot set both 'expression' AND 'shouldLogFullMessage' properties");
		this.shouldLogFullMessageSet = true;
		this.expression =
				shouldLogFullMessage
						? new FunctionExpression<Message<?>>(Function.identity())
						: new FunctionExpression<Message<?>>(Message::getPayload);
	}

	@Override
	public String getComponentType() {
		return "logging-channel-adapter";
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Supplier<CharSequence> logMessage = () -> createLogMessage(message);
		switch (this.level) {
			case FATAL:
				this.messageLogger.fatal(logMessage);
				break;
			case ERROR:
				this.messageLogger.error(logMessage);
				break;
			case WARN:
				this.messageLogger.warn(logMessage);
				break;
			case INFO:
				this.messageLogger.info(logMessage);
				break;
			case DEBUG:
				this.messageLogger.debug(logMessage);
				break;
			case TRACE:
				this.messageLogger.trace(logMessage);
				break;
			default:
				throw new IllegalStateException("Level '" + this.level + "' is not supported");
		}
	}

	@Nullable
	private String createLogMessage(Message<?> message) {
		Object logMessage = this.expression.getValue(this.evaluationContext, message);
		return logMessage instanceof Throwable
				? createLogMessage((Throwable) logMessage)
				: Objects.toString(logMessage);
	}

	private String createLogMessage(Throwable throwable) {
		StringWriter stringWriter = new StringWriter();
		if (throwable instanceof AggregateMessageDeliveryException) {
			stringWriter.append(throwable.getMessage());
			for (Exception exception : ((AggregateMessageDeliveryException) throwable).getAggregatedExceptions()) {
				printStackTrace(exception, stringWriter);
			}
		}
		else {
			printStackTrace(throwable, stringWriter);
		}
		return stringWriter.toString();
	}

	private void printStackTrace(Throwable throwable, Writer writer) {
		throwable.printStackTrace(new PrintWriter(writer, true));
	}

}
