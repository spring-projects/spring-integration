/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.routingslip;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.messaging.Message;

/**
 * The {@link Expression} based {@link RoutingSlipRouteStrategy} implementation.
 * The {@code requestMessage} and {@code reply} object are wrapped
 * to the {@link RequestAndReply} which is used as a {@link EvaluationContext} {@code rootObject}.
 * This is necessary to avoid a creation of a new {@link EvaluationContext} on each invocation
 * when additional parameter can be populated as expression variable, but {@link EvaluationContext}
 * isn't thread-safe.
 * <p>
 * The {@link ExpressionEvaluatingRoutingSlipRouteStrategy} can be used directly as a regular bean
 * in the {@code ApplicationContext} and its {@code beanName} can be used from {@code routingSlip}
 * header configuration.
 * <p>
 * Usage of {@link ExpressionEvaluatingRoutingSlipRouteStrategy} as a regular bean definition is
 * a recommended way in case of distributed environment, when message with {@code routingSlip}
 * header can be sent across the network. One of this case is a {@code QueueChannel} with
 * persistent {@code MessageStore}, when {@link ExpressionEvaluatingRoutingSlipRouteStrategy}
 * instance as a header value will be non-serializable.
 * <p>
 * This class is used internally from {@code RoutingSlipHeaderValueMessageProcessor}
 * to populate {@code routingSlip} header value item, when the {@code value}
 * from configuration contains expression definitions:
 * <pre class="code">
 * {@code
 * <header-enricher>
 *     <routing-slip
 *           value="channel1; @routingSlipPojo.get(request, reply); request.headers[foo]"/>
 * </header-enricher>
 * }
 * </pre>
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class ExpressionEvaluatingRoutingSlipRouteStrategy
		implements RoutingSlipRouteStrategy, BeanFactoryAware, InitializingBean {

	private static final ExpressionParser PARSER = new SpelExpressionParser();

	private final Expression expression;

	private EvaluationContext evaluationContext;

	private BeanFactory beanFactory;

	public ExpressionEvaluatingRoutingSlipRouteStrategy(String expression) {
		this(PARSER.parseExpression(expression));
	}

	public ExpressionEvaluatingRoutingSlipRouteStrategy(Expression expression) {
		this.expression = expression;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
	}

	@Override
	public Object getNextPath(Message<?> requestMessage, Object reply) {
		return this.expression.getValue(this.evaluationContext, new RequestAndReply(requestMessage, reply));
	}

	@Override
	public String toString() {
		return "ExpressionEvaluatingRoutingSlipRouteStrategy for: [" + this.expression.getExpressionString() + "]";
	}

	public static class RequestAndReply {

		private final Message<?> request;

		private final Object reply;

		RequestAndReply(Message<?> request, Object reply) {
			this.request = request;
			this.reply = reply;
		}

		public Message<?> getRequest() {
			return this.request;
		}

		public Object getReply() {
			return this.reply;
		}

	}

}
