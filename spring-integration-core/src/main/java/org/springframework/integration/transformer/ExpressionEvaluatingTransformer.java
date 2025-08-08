/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;

/**
 * A Message Transformer implementation that evaluates the specified SpEL
 * expression. The result of evaluation will typically be considered as the
 * payload of a new Message unless it is itself already a Message.
 *
 * @author Mark Fisher
 * @since 2.0
 */
public class ExpressionEvaluatingTransformer extends AbstractMessageProcessingTransformer {

	public ExpressionEvaluatingTransformer(Expression expression) {
		super(new ExpressionEvaluatingMessageProcessor<Object>(expression));
	}

}
