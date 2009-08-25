/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParserConfiguration;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.gateway.SimpleMessageMapper;
import org.springframework.integration.message.InboundMessageMapper;
import org.springframework.util.Assert;

/**
 * A {@link MethodInterceptor} that publishes Messages to a channel. The
 * payload of the published Message can be derived from arguments or any return
 * value or exception resulting from the method invocation. That mapping is the
 * responsibility of the EL expression provided by the ExpressionSource.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MessagePublishingInterceptor implements MethodInterceptor {

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private final ExpressionSource expressionSource;

	private final ExpressionParser parser = new SpelExpressionParser(
			SpelExpressionParserConfiguration.CreateObjectIfAttemptToReferenceNull |
			SpelExpressionParserConfiguration.GrowListsOnIndexBeyondSize);

	private final InboundMessageMapper<Object> messageMapper = new SimpleMessageMapper();

	private volatile ChannelResolver channelResolver;


	public MessagePublishingInterceptor(ExpressionSource expressionSource) {
		Assert.notNull(expressionSource, "expressionSource must not be null");
		this.expressionSource = expressionSource;
	}


	public void setDefaultChannel(MessageChannel defaultChannel) {
		this.channelTemplate.setDefaultChannel(defaultChannel);
	}

	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public final Object invoke(final MethodInvocation invocation) throws Throwable {
		final StandardEvaluationContext context = new StandardEvaluationContext();
		context.addPropertyAccessor(new MapAccessor());
		Class<?> targetClass = AopUtils.getTargetClass(invocation.getThis());
		Method method = AopUtils.getMostSpecificMethod(invocation.getMethod(), targetClass);
		String[] argumentNames = this.expressionSource.getArgumentNames(method);
		if (invocation.getArguments().length > 0 && argumentNames != null) {
			int index = 0;
			Map<String, Object> argumentMap = new HashMap<String, Object>();
			for (String argumentName : argumentNames) {
				if (invocation.getArguments().length <= index) {
					break;
				}
				argumentMap.put(argumentName, invocation.getArguments()[index++]);
			}
			context.setVariable(this.expressionSource.getArgumentMapName(method), argumentMap);
		}
		try {
			Object returnValue = invocation.proceed();
			context.setVariable(this.expressionSource.getReturnValueName(method), returnValue);
			return returnValue;
		}
		catch (Throwable t) {
			context.setVariable(this.expressionSource.getExceptionName(method), t);
			throw t;
		}
		finally {
			publishMessage(method, context);
		}
	}

	private void publishMessage(Method method, EvaluationContext context) throws Exception {
		String expressionString = this.expressionSource.getExpressionString(method);
		if (expressionString != null) {
			Expression expression = this.parser.parseExpression(expressionString);
			Object result = expression.getValue(context);
			if (result != null) {
				Message<?> message = this.messageMapper.toMessage(result);
				String channelName = this.expressionSource.getChannelName(method);
				MessageChannel channel = null;
				if (channelName != null) {
					Assert.state(this.channelResolver != null, "ChannelResolver is required to resolve channel names.");
					channel = this.channelResolver.resolveChannelName(channelName);
				}
				if (channel != null) {
					this.channelTemplate.send(message, channel);
				}
				else {
					this.channelTemplate.send(message);
				}
			}
		}
	}

}
