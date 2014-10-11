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
package org.springframework.integration.ftp.session;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryResolver;
import org.springframework.messaging.Message;

/**
 * Ftp {@link SessionFactoryResolver}; Used to resolve sessionfactory from message sent
 * containing host, username, password, name.
 *
 * @author David Liu
 * @since 4.1
 *
 */
public class FtpSessionFactoryResolver<F> implements SessionFactoryResolver<F>, IntegrationEvaluationContextAware{

	private final static String DEFAULT_HOST_EXPRESSION_STRING = "headers['ftp_host']";

	private final static String DEFAULT_USER_EXPRESSION_STRING = "headers['ftp_username']";

	private final static String DEFAULT_PASSWORD_EXPRESSION_STRING = "headers['ftp_password']";

	private final static String DEFAULT_PORT_EXPRESSION_STRING = "headers['ftp_port']";

	private final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private volatile Expression headerExpression = expressionParser.parseExpression(DEFAULT_HOST_EXPRESSION_STRING);

	private volatile Expression userNameExpression = expressionParser.parseExpression(DEFAULT_USER_EXPRESSION_STRING);

	private volatile Expression passwordExpression = expressionParser.parseExpression(DEFAULT_PASSWORD_EXPRESSION_STRING);

	private volatile Expression portExpression = expressionParser.parseExpression(DEFAULT_PORT_EXPRESSION_STRING);

	private EvaluationContext evaluationContext;

	public void setHostExpression(String hostExpression) {
		this.headerExpression = expressionParser.parseExpression(hostExpression);
	}

	public void setUserNameExpression(String userExpression) {
		this.userNameExpression = expressionParser.parseExpression(userExpression);
	}

	public void setPasswordExpression(String passwordExpression) {
		this.passwordExpression = expressionParser.parseExpression(passwordExpression);
	}

	public void setPortExpression(String portExpression) {
		this.portExpression = expressionParser.parseExpression(portExpression);
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SessionFactory<F> resolve(Message<?> message) {
		DefaultFtpSessionFactory sessionFactory = new DefaultFtpSessionFactory();
		sessionFactory.setHost(evaluationContext != null ? this.headerExpression.getValue(evaluationContext, message, String.class)
				: this.headerExpression.getValue(message, String.class));
		sessionFactory.setUsername(evaluationContext != null ? this.userNameExpression.getValue(evaluationContext, message, String.class)
				 : this.userNameExpression.getValue(message, String.class));
		sessionFactory.setPassword(evaluationContext != null ? this.passwordExpression.getValue(evaluationContext, message, String.class)
				 : this.passwordExpression.getValue(message, String.class));
		sessionFactory.setPort(evaluationContext != null ? this.portExpression.getValue(evaluationContext, message, Integer.class)
				 : this.portExpression.getValue(message, Integer.class));
		return (SessionFactory<F>) sessionFactory;
	}

}
