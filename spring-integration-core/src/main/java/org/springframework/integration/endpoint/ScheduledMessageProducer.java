/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.endpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.context.SimpleBeanResolver;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class ScheduledMessageProducer extends MessageProducerSupport {

	private static final ExpressionParser PARSER = new SpelExpressionParser();


	private final Trigger trigger;

	private final MessageProducingTask task;

	private volatile ScheduledFuture<?> future;

	private volatile Map<String, Expression> headerExpressionMap;

	private final StandardEvaluationContext context = new StandardEvaluationContext();


	public ScheduledMessageProducer(Trigger trigger, String payloadExpression) {
		Assert.notNull(trigger, "trigger must not be null");
		Assert.hasText(payloadExpression, "payloadExpression is required");
		this.trigger = trigger;
		this.task = new MessageProducingTask(PARSER.parseExpression(payloadExpression));
	}


	public void setHeaderExpressions(Map<String, String> headerExpressions) {
		if (headerExpressions != null) {
			Map<String, Expression> parsedExpressions = new HashMap<String, Expression>();
			for (Map.Entry<String, String> entry : headerExpressions.entrySet()) {
				parsedExpressions.put(entry.getKey(), PARSER.parseExpression(entry.getValue()));
			}
			this.headerExpressionMap = parsedExpressions;
		}
	}

	private Map<String, Object> evaluateHeaders() {
		Map<String, Object> headers = new HashMap<String, Object>();
		if (this.headerExpressionMap != null) {
			for (Map.Entry<String, Expression> entry : this.headerExpressionMap.entrySet()) {
				headers.put(entry.getKey(), entry.getValue().getValue(context));
			}
		}
		return headers;
	}

	@Override
	protected void onInit() {
		super.onInit();
		final BeanFactory beanFactory = this.getBeanFactory();
		if (beanFactory != null) {
			this.context.setBeanResolver(new SimpleBeanResolver(beanFactory));
		}
	}

	@Override
	protected void doStart() {
		this.future = this.getTaskScheduler().schedule(this.task, this.trigger);
	}

	@Override
	protected void doStop() {
		if (this.future != null) {
			this.future.cancel(true);
		}
	}


	private class MessageProducingTask implements Runnable {

		private final Expression payloadExpression;


		private MessageProducingTask(Expression payloadExpression) {
			this.payloadExpression = payloadExpression;
		}


		public void run() {
			Object payload = this.payloadExpression.getValue(context);
			if (payload != null) {
				Map<String, Object> headers = evaluateHeaders();
				MessageBuilder<?> builder = MessageBuilder.withPayload(payload);
				if (!CollectionUtils.isEmpty(headers)) {
					builder.copyHeaders(headers);
				}
				sendMessage(builder.build());
			}
		}
	}

}
