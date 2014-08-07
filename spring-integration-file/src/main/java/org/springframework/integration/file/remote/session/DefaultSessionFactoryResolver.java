/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.file.remote.session;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;

/**
 * @author David Liu
 * @since 4.1
 *
 */
public class DefaultSessionFactoryResolver<F> implements SessionFactoryResolver<F>, BeanFactoryAware {

	private final static String SESSIONFACTORY = "headers['sessionFactory']";

	private final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private volatile Expression sessionFactoryExpression = expressionParser.parseExpression(SESSIONFACTORY);

	private volatile BeanFactory beanFactory;

	public void setSessionFactory(String expression) {
		this.sessionFactoryExpression = expressionParser.parseExpression(expression);
	}

	@SuppressWarnings("unchecked")
	@Override
	public SessionFactory<F> resolve(Message<?> message) {
		if(beanFactory != null) {
			return beanFactory.getBean(this.sessionFactoryExpression.getValue(message).toString(), SessionFactory.class);
		}
		return null;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
