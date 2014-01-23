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

package org.springframework.integration.redis.outbound;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class ExpressionArgumentsStrategy implements ArgumentsStrategy, IntegrationEvaluationContextAware, BeanFactoryAware {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Expression[] argumentExpressions;

	private EvaluationContext evaluationContext;

	private final boolean useCommandVariable;

	private BeanFactory beanFactory;

	public ExpressionArgumentsStrategy(String[] argumentExpressions) {
		this(argumentExpressions, false);
	}

	public ExpressionArgumentsStrategy(String[] argumentExpressions, boolean useCommandVariable) {
		Assert.notNull(argumentExpressions, "'argumentExpressions' must not be null");
		Assert.noNullElements(argumentExpressions, "'argumentExpressions' cannot have null values.");
		List<Expression> expressions = new LinkedList<Expression>();
		for (String argumentExpression : argumentExpressions) {
			expressions.add(PARSER.parseExpression(argumentExpression));
		}
		this.argumentExpressions = expressions.toArray(new Expression[expressions.size()]);
		this.useCommandVariable = useCommandVariable;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		Assert.notNull(evaluationContext, "'evaluationContext' must not be null");
		this.evaluationContext = evaluationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object[] resolve(String command, Message<?> message) {
		EvaluationContext evaluationContext = this.evaluationContext;

		if (this.useCommandVariable) {
			evaluationContext = IntegrationContextUtils.getEvaluationContext(this.beanFactory);
			evaluationContext.setVariable("cmd", command);
		}

		List<Object> arguments = new ArrayList<Object>();
		for (Expression argumentExpression : this.argumentExpressions) {
			Object argument = argumentExpression.getValue(evaluationContext, message);
			if (argument != null) {
				arguments.add(argument);
			}
		}
		return arguments.toArray();
	}

}
