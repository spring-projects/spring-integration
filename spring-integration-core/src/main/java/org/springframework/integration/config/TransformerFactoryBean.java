/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.expression.Expression;
import org.springframework.integration.handler.AbstractMessageProducingHandler;
import org.springframework.integration.transformer.ExpressionEvaluatingTransformer;
import org.springframework.integration.transformer.MessageTransformingHandler;
import org.springframework.integration.transformer.MethodInvokingTransformer;
import org.springframework.integration.transformer.Transformer;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean for creating a Message Transformer.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 */
public class TransformerFactoryBean extends AbstractStandardMessageHandlerFactoryBean {

	public TransformerFactoryBean() {
		setRequiresReply(true);
	}

	@Override
	protected MessageHandler createMethodInvokingHandler(Object targetObject, String targetMethodName) {
		Assert.notNull(targetObject, "targetObject must not be null");
		Transformer transformer = null;
		if (targetObject instanceof Transformer) {
			transformer = (Transformer) targetObject;
		}
		else {
			this.checkForIllegalTarget(targetObject, targetMethodName);
			if (StringUtils.hasText(targetMethodName)) {
				transformer = new MethodInvokingTransformer(targetObject, targetMethodName);
			}
			else {
				transformer = new MethodInvokingTransformer(targetObject);
			}
		}
		return this.createHandler(transformer);
	}

	@Override
	protected MessageHandler createExpressionEvaluatingHandler(Expression expression) {
		Transformer transformer = new ExpressionEvaluatingTransformer(expression);
		MessageTransformingHandler handler = this.createHandler(transformer);
		handler.setPrimaryExpression(expression);
		return handler;
	}

	protected MessageTransformingHandler createHandler(Transformer transformer) {
		MessageTransformingHandler handler = new MessageTransformingHandler(transformer);
		this.postProcessReplyProducer(handler);
		return handler;
	}

	/**
	 * Always returns true - any {@link AbstractMessageProducingHandler} can
	 * be used directly.
	 */
	@Override
	protected boolean canBeUsedDirect(AbstractMessageProducingHandler handler) {
		return true; // Any AMPH can be a transformer
	}

	@Override
	protected Class<? extends MessageHandler> getPreCreationHandlerType() {
		return MessageTransformingHandler.class;
	}

}
