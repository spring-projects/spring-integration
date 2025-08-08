/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file;

import java.io.File;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
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
 * @author Artem Bilan
 */
public class DefaultFileNameGenerator extends AbstractExpressionEvaluator implements FileNameGenerator {

	private volatile Expression expression =
			new FunctionExpression<Message<?>>((message) -> message.getHeaders().get(FileHeaders.FILENAME));

	/**
	 * Specify an expression to be evaluated against the Message
	 * in order to generate a file name.
	 * @param expression The expression.
	 */
	public void setExpression(String expression) {
		Assert.hasText(expression, "expression must not be empty");
		this.expression = EXPRESSION_PARSER.parseExpression(expression);
	}

	/**
	 * Specify a custom header name to check for the file name.
	 * The default is defined by {@link FileHeaders#FILENAME}.
	 * @param headerName The header name.
	 */
	public void setHeaderName(String headerName) {
		Assert.notNull(headerName, "'headerName' must not be null");
		this.expression = new FunctionExpression<Message<?>>((message) -> message.getHeaders().get(headerName));
	}

	@Override
	public String generateFileName(Message<?> message) {
		Object filename = evaluateExpression(this.expression, message);
		if (filename instanceof String name && StringUtils.hasText(name)) {
			return name;
		}
		Object payload = message.getPayload();
		if (payload instanceof File file) {
			return file.getName();
		}
		return message.getHeaders().getId() + ".msg";
	}

}
