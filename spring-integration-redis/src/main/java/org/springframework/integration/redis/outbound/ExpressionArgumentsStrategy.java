/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.outbound;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class ExpressionArgumentsStrategy implements ArgumentsStrategy, BeanFactoryAware, InitializingBean {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Expression[] argumentExpressions;

	private final boolean useCommandVariable;

	@SuppressWarnings("NullAway.Init")
	private EvaluationContext evaluationContext;

	@SuppressWarnings("NullAway.Init")
	private BeanFactory beanFactory;

	public ExpressionArgumentsStrategy(String[] argumentExpressions) {
		this(argumentExpressions, false);
	}

	public ExpressionArgumentsStrategy(String[] argumentExpressions, boolean useCommandVariable) {
		Assert.notNull(argumentExpressions, "'argumentExpressions' must not be null");
		Assert.noNullElements(argumentExpressions, "'argumentExpressions' cannot have null values.");
		List<Expression> expressions = new LinkedList<>();
		for (String argumentExpression : argumentExpressions) {
			expressions.add(PARSER.parseExpression(argumentExpression));
		}
		this.argumentExpressions = expressions.toArray(new Expression[0]);
		this.useCommandVariable = useCommandVariable;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
	}

	@Override
	public Object[] resolve(String command, Message<?> message) {
		EvaluationContext evaluationContextToUse = this.evaluationContext;

		if (this.useCommandVariable) {
			evaluationContextToUse = IntegrationContextUtils.getEvaluationContext(this.beanFactory);
			evaluationContextToUse.setVariable("cmd", command);
		}
		List<Object> arguments = new ArrayList<>(this.argumentExpressions.length);
		for (Expression argumentExpression : this.argumentExpressions) {
			Object argument = argumentExpression.getValue(evaluationContextToUse, message);
			if (argument != null) {
				arguments.add(argument);
			}
		}
		return arguments.toArray();
	}

}
