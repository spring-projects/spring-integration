/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * FactoryBean for creating Expression instances.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ExpressionFactoryBean extends AbstractFactoryBean<Expression> {

	private static final ExpressionParser DEFAULT_PARSER = new SpelExpressionParser();

	private final String expressionString;

	private ExpressionParser parser = DEFAULT_PARSER;

	public ExpressionFactoryBean(String expressionString) {
		Assert.hasText(expressionString, "'expressionString' must not be empty or null");
		this.expressionString = expressionString;
	}

	public void setParserConfiguration(SpelParserConfiguration parserConfiguration) {
		Assert.notNull(parserConfiguration, "'parserConfiguration' must not be null");
		this.parser = new SpelExpressionParser(parserConfiguration);
	}

	@Override
	public Class<?> getObjectType() {
		return Expression.class;
	}

	@Override
	protected Expression createInstance() {
		return this.parser.parseExpression(this.expressionString);
	}

}
