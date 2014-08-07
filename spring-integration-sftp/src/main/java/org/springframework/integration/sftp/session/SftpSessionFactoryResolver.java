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
package org.springframework.integration.sftp.session;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.session.SessionFactoryResolver;
import org.springframework.messaging.Message;

/**
 * @author David Liu
 * @since 4.1
 *
 */
public class SftpSessionFactoryResolver<F> implements SessionFactoryResolver<F>{

	private final static String DEFAULTHOSTEXPRESSIONSTRING = "headers['ftp_host']";

	private final static String DEFAULTUSEREXPRESSIONSTRING = "headers['ftp_username']";

	private final static String DEFAULTPASSWORDEXPRESSIONSTRING = "headers['ftp_password']";

	private final static String DEFAULTPORTEXPRESSIONSTRING = "headers['ftp_port']";

	private final ExpressionParser expressionParser = new SpelExpressionParser(new SpelParserConfiguration(true, true));

	private volatile Expression headerExpression = expressionParser.parseExpression(DEFAULTHOSTEXPRESSIONSTRING);

	private volatile Expression userNameExpression = expressionParser.parseExpression(DEFAULTUSEREXPRESSIONSTRING);

	private volatile Expression passwordExpression = expressionParser.parseExpression(DEFAULTPASSWORDEXPRESSIONSTRING);

	private volatile Expression portExpression = expressionParser.parseExpression(DEFAULTPORTEXPRESSIONSTRING);


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

	@SuppressWarnings("unchecked")
	@Override
	public SessionFactory<F> resolve(Message<?> message) {
		DefaultSftpSessionFactory sessionFactory = new DefaultSftpSessionFactory();
		sessionFactory.setHost(this.headerExpression.getValue(message).toString());
		sessionFactory.setUser(this.userNameExpression.getValue(message).toString());
		sessionFactory.setPassword(this.passwordExpression.getValue(message).toString());
		sessionFactory.setPort(Integer.valueOf(this.portExpression.getValue(message).toString()));
		return (SessionFactory<F>) sessionFactory;
	}

}
