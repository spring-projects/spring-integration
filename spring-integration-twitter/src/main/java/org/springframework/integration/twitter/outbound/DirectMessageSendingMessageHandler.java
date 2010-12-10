/*
 * Copyright 2002-2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.twitter.outbound;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;

/**
 * Simple adapter to support sending outbound direct messages ("DM"s) using Twitter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class DirectMessageSendingMessageHandler extends AbstractMessageHandler {

	private final TwitterOperations twitterOperations;
	private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
	private static final ExpressionParser PARSER = new SpelExpressionParser();
	
	private volatile Expression targetUserExpression;


	public DirectMessageSendingMessageHandler(TwitterOperations twitterOperations) {
		Assert.notNull(twitterOperations, "twitterOperations must not be null");
		this.twitterOperations = twitterOperations;
	}

	public void setTargetUserExpression(Expression targetUserExpression) {
		Assert.notNull(targetUserExpression, "'targetUserExpression' must not be null");
		this.targetUserExpression = targetUserExpression;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isInstanceOf(String.class, message.getPayload(), "Only payload of type String is supported. If your payload " +
				"is not of type String consider adding a transformer to the message flow in front of this adapter.");
//		Assert.isTrue(message.getHeaders().containsKey(TwitterHeaders.DM_TARGET_USER_ID), 
//				"the '" + TwitterHeaders.DM_TARGET_USER_ID + "' header is required");
		
		Object toUser = targetUserExpression.getValue(this.evaluationContext, message);
		
		Assert.isTrue(toUser instanceof String || toUser instanceof Integer,
				"the header '" + TwitterHeaders.DM_TARGET_USER_ID + 
				"' must be either a String (a screenname) or an int (a user ID)");
		String payload = (String) message.getPayload();
		if (toUser instanceof Integer) {
			this.twitterOperations.sendDirectMessage((Integer) toUser, payload);
		} 
		else if (toUser instanceof String) {
			this.twitterOperations.sendDirectMessage((String) toUser, payload);
		}
	}
	
	@Override
	public void onInit() throws Exception{
		super.onInit();
		BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		ConversionService conversionService = this.getConversionService();
		if (conversionService != null) {
			this.evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		if (targetUserExpression == null){
			targetUserExpression = 
				PARSER.parseExpression("headers[T(org.springframework.integration.twitter.core.TwitterHeaders).DM_TARGET_USER_ID]");
		}
	}

}
