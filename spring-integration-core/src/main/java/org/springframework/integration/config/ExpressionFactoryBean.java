/*
 * Copyright 2002-2024 the original author or authors.
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
