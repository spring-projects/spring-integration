/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHistoryEvent;
import org.springframework.util.StringUtils;

/**
 * MessageHandler implementation that simply logs the Message or its payload
 * depending on the value of the 'shouldLogFullMessage' property. If logging
 * the payload, and it is assignable to Throwable, it will log the stack trace.
 * By default, it will log the payload only.
 * 
 * @author Mark Fisher
 * @since 1.0.1
 */
public class LoggingHandler extends AbstractMessageHandler {

	private static enum Level { FATAL, ERROR, WARN, INFO, DEBUG, TRACE }

	private static final String COMPONENT_TYPE_LABEL = "logging-channel-adapter";


	private boolean shouldLogFullMessage;

	private final Level level;


	/**
	 * Create a LoggingHandler with the given log level (case-insensitive).
	 * <p>The valid levels are: FATAL, ERROR, WARN, INFO, DEBUG, or TRACE
	 */
	public LoggingHandler(String level) {
		try {
			this.level = Level.valueOf(level.toUpperCase());
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid log level '" + level +
					"'. The (case-insensitive) supported values are: " + StringUtils.arrayToCommaDelimitedString(Level.values()));
		}
	}


	/**
	 * Specify whether to log the full Message. Otherwise, only the payload
	 * will be logged. This value is <code>false</code> by default.
	 */
	public void setShouldLogFullMessage(boolean shouldLogFullMessage) {
		this.shouldLogFullMessage = shouldLogFullMessage;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object logMessage = (this.shouldLogFullMessage) ? message : message.getPayload();
		if (logMessage instanceof Throwable) {
			StringWriter stringWriter = new StringWriter();
			((Throwable) logMessage).printStackTrace(new PrintWriter(stringWriter, true));
			logMessage = stringWriter.toString();
		}
		switch (this.level) {
			case FATAL :
				if (logger.isFatalEnabled()) {
					logger.fatal(logMessage);
				}
				break;
			case ERROR :
				if (logger.isErrorEnabled()) {
					logger.error(logMessage);
				}
				break;
			case WARN :
				if (logger.isWarnEnabled()) {
					logger.warn(logMessage);
				}
				break;
			case INFO :
				if (logger.isInfoEnabled()) {
					logger.info(logMessage);
				}
				break;
			case DEBUG :
				if (logger.isDebugEnabled()) {
					logger.debug(logMessage);
				}
				break;
			case TRACE :
				if (logger.isTraceEnabled()) {
					logger.trace(logMessage);
				}
				break;
		}
	}

	@Override
	protected void postProcessHistoryEvent(MessageHistoryEvent e) {
		e.setComponentType(COMPONENT_TYPE_LABEL);
	}

}
