/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.gemfire.outbound;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.geode.GemFireCheckedException;
import org.apache.geode.GemFireException;
import org.apache.geode.cache.Region;

import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that writes to a GemFire Region. The
 * Message's payload must be an instance of {@link Map} or {@link #cacheEntryExpressions}
 * must be provided.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class CacheWritingMessageHandler extends AbstractMessageHandler {

	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final Map<Expression, Expression> cacheEntryExpressions = new LinkedHashMap<Expression, Expression>();

	private final GemfireTemplate gemfireTemplate = new GemfireTemplate();

	private volatile EvaluationContext evaluationContext;

	@SuppressWarnings("rawtypes")
	public CacheWritingMessageHandler(Region region) {
		Assert.notNull(region, "region must not be null");
		this.gemfireTemplate.setRegion(region);
	}

	@Override
	public String getComponentType() {
		return "gemfire:outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		this.gemfireTemplate.afterPropertiesSet();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void handleMessageInternal(Message<?> message) {
		Object payload = message.getPayload();
		Map<?, ?> cacheValues = (this.cacheEntryExpressions.size() > 0) ? evaluateCacheEntries(message) : null;

		if (cacheValues == null) {
			Assert.state(payload instanceof Map,
					"If cache entry expressions are not configured, then payload must be a Map");
			cacheValues = (Map) payload;
		}

		final Map<?, ?> map = cacheValues;

		this.gemfireTemplate.execute(new GemfireCallback<Object>() {

			@Override
			public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
				region.putAll(map);
				return null;
			}

		});
	}

	private Map<Object, Object> evaluateCacheEntries(Message<?> message) {
		if (this.cacheEntryExpressions.size() == 0) {
			return null;
		}
		else {
			Map<Object, Object> cacheValues = new HashMap<Object, Object>();
			for (Entry<Expression, Expression> expressionEntry : this.cacheEntryExpressions.entrySet()) {
				cacheValues.put(expressionEntry.getKey().getValue(this.evaluationContext, message),
						expressionEntry.getValue().getValue(this.evaluationContext, message));
			}
			return cacheValues;
		}
	}

	public void setCacheEntries(Map<String, String> cacheEntries) {
		Assert.notNull(cacheEntries, "'cacheEntries' must not be null");
		if (this.cacheEntryExpressions.size() > 0) {
			this.cacheEntryExpressions.clear();
		}

		for (Entry<String, String> cacheEntry : cacheEntries.entrySet()) {
			this.cacheEntryExpressions.put(PARSER.parseExpression(cacheEntry.getKey()),
					PARSER.parseExpression(cacheEntry.getValue()));
		}
	}

	public void setCacheEntryExpressions(Map<Expression, Expression> cacheEntryExpressions) {
		Assert.notNull(cacheEntryExpressions, "'cacheEntryExpressions' must not be null");
		if (this.cacheEntryExpressions.size() > 0) {
			this.cacheEntryExpressions.clear();
		}
		this.cacheEntryExpressions.putAll(cacheEntryExpressions);
	}

}
