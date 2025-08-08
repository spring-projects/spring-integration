/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstraction of Jpa parameters allowing to provide static parameters
 * and SpEl Expression based parameters.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.2
 *
 */
public class JpaParameter {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private String name;

	private Object value;

	private String expression;

	private Expression spelExpression;

	private Expression projectionExpression;

	/**
	 * Default constructor.
	 */
	public JpaParameter() {
	}

	/**
	 * Instantiates a new Jpa Parameter.
	 * @param name Name of the JPA parameter, must not be null or empty
	 * @param value If null, the expression property must be set
	 * @param expression If null, the value property must be set
	 */
	public JpaParameter(String name, @Nullable Object value, @Nullable String expression) {
		Assert.hasText(name, "'name' must not be empty.");
		this.name = name;
		this.value = value;
		setExpression(expression);
	}

	/**
	 * Instantiates a new Jpa Parameter without a name. This is useful for specifying
	 * positional Jpa parameters.
	 * @param value If null, the expression property must be set
	 * @param expression If null, the value property must be set
	 */
	public JpaParameter(@Nullable Object value, @Nullable String expression) {
		this.value = value;
		setExpression(expression);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Nullable
	public Object getValue() {
		return this.value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	@Nullable
	public String getExpression() {
		return this.expression;
	}

	@Nullable
	public Expression getSpelExpression() {
		return this.spelExpression;
	}

	@Nullable
	public Expression getProjectionExpression() {
		return this.projectionExpression;
	}

	public final void setExpression(@Nullable String expression) {
		if (expression != null) {
			this.expression = expression;
			this.spelExpression = PARSER.parseExpression(expression);
			this.projectionExpression = PARSER.parseExpression("#root.![" + expression + "]");
		}
	}

	@Override
	public String toString() {
		return "JpaParameter [name=" + this.name +
				", value=" + this.value +
				", expression=" + this.expression +
				"]";
	}

}
