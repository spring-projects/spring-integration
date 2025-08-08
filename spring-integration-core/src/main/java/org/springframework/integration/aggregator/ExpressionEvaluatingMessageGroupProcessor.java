/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import java.util.Map;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} implementation that evaluates a SpEL expression. The SpEL context root is the list of
 * all Messages in the group. The evaluation result can be any Object and is send as new Message payload to the output
 * channel.
 *
 * @author Alex Peters
 * @author Dave Syer
 * @author Gary Russell
 */
public class ExpressionEvaluatingMessageGroupProcessor extends AbstractAggregatingMessageGroupProcessor {

	private final ExpressionEvaluatingMessageListProcessor processor;

	public ExpressionEvaluatingMessageGroupProcessor(String expression) {
		this.processor = new ExpressionEvaluatingMessageListProcessor(expression);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		this.processor.setBeanFactory(beanFactory);
	}

	public void setConversionService(ConversionService conversionService) {
		this.processor.setConversionService(conversionService);
	}

	public void setExpectedType(Class<?> expectedType) {
		this.processor.setExpectedType(expectedType);
	}

	/**
	 * Evaluate the expression provided on the messages (a collection) in the group, and delegate to the
	 * {@link org.springframework.integration.core.MessagingTemplate} to send downstream.
	 */
	@Override
	protected Object aggregatePayloads(MessageGroup group, Map<String, Object> headers) {
		return this.processor.process(group.getMessages());
	}

}
