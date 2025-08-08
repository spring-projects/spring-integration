/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.expression;

import java.util.Locale;

import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for retrieving Expressions.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.0
 */
@FunctionalInterface
public interface ExpressionSource {

	@Nullable
	Expression getExpression(String key, Locale locale);

}
