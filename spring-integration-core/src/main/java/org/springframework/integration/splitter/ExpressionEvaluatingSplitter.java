/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.splitter;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A Message Splitter implementation that evaluates the specified SpEL
 * expression. The result of evaluation will typically be a Collection or
 * Array. If the result is not a Collection or Array, then the single Object
 * will be returned as the payload of a single reply Message.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class ExpressionEvaluatingSplitter extends AbstractMessageProcessingSplitter {

	@SuppressWarnings({"unchecked", "rawtypes"})
	public ExpressionEvaluatingSplitter(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor(expression));
		setPrimaryExpression(expression);
	}

}
