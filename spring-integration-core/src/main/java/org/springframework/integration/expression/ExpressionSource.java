/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.expression;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

import org.springframework.expression.Expression;

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
