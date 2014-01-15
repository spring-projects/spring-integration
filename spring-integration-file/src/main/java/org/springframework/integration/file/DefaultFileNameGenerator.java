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

package org.springframework.integration.file;

import java.io.File;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the filename generator strategy. It evaluates an
 * expression against the Message in order to generate the file name. Either
 * the 'expression' property can be set directly, or for a simple header name
 * to be used as the filename, there is also a {@link #setHeaderName(String)}
 * method for convenience. If neither a header name nor custom expression is set,
 * the default header name is defined by the constant {@link FileHeaders#FILENAME}.
 * If no String-typed value is returned from the expression evaluation (or
 * associated with the header if no expression has been provided), it checks if
 * the Message payload is a File instance, and if so, it uses the same name.
 * Finally, it falls back to the Message ID and adds the suffix '.msg'.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class DefaultFileNameGenerator extends AbstractExpressionEvaluator implements FileNameGenerator {

	private static final String DEFAULT_EXPRESSION = "headers['" + FileHeaders.FILENAME + "']";

	private final static ExpressionParser parser = new SpelExpressionParser();

	private volatile Expression expression = parser.parseExpression(DEFAULT_EXPRESSION);

	/**
	 * Specify an expression to be evaluated against the Message
	 * in order to generate a file name.
	 *
	 * @param expression The expression.
	 */
	public void setExpression(String expression) {
		Assert.hasText(expression, "expression must not be empty");
		this.expression = parser.parseExpression(expression);
	}

	/**
	 * Specify a custom header name to check for the file name.
	 * The default is defined by {@link FileHeaders#FILENAME}.
	 *
	 * @param headerName The header name.
	 */
	public void setHeaderName(String headerName) {
		Assert.notNull(headerName, "'headerName' must not be null");
		this.expression = parser.parseExpression("headers['" + headerName + "']");
	}

	@Override
	public String generateFileName(Message<?> message) {
		Object filenameProperty = this.evaluateExpression(this.expression, message);
		if (filenameProperty instanceof String && StringUtils.hasText((String) filenameProperty)) {
			return (String) filenameProperty;
		}
		if (message.getPayload() instanceof File) {
			return ((File) message.getPayload()).getName();
		}
		return message.getHeaders().getId() + ".msg";
	}

}
