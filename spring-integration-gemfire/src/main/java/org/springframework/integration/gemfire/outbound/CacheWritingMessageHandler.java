/*
 * Copyright 2002-2013 the original author or authors.
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

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Region;

import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} implementation that writes to a GemFire Region. The
 * Message's payload must be an instance of java.util.Map.
 *
 * @author Mark Fisher
 * @author David Turanski
 * @since 2.1
 */
public class CacheWritingMessageHandler extends AbstractMessageHandler {
	private final Map<Expression, Expression> cacheEntryExpressions = new LinkedHashMap<Expression, Expression>();

	private final GemfireTemplate gemfireTemplate = new GemfireTemplate();

	@SuppressWarnings("rawtypes")
	public CacheWritingMessageHandler(Region region) {
		Assert.notNull(region, "region must not be null");
		this.gemfireTemplate.setRegion(region);
		this.gemfireTemplate.afterPropertiesSet();
	}

	@Override
	public String getComponentType() {
		return "gemfire:outbound-channel-adapter";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleMessageInternal(Message<?> message) {
		Object payload = message.getPayload();
		Map<?, ?> cacheValues = (cacheEntryExpressions.size() > 0) ? parseCacheEntries(message) : null;

		if (cacheValues == null) {
			Assert.isTrue(payload instanceof Map,
					"If cache entry expressions are not configured, then payload must be a Map");
			cacheValues = (Map<?, ?>) payload;
		}

		final Map<?, ?> map = cacheValues;

		this.gemfireTemplate.execute(new GemfireCallback<Object>() {
			@Override
			@SuppressWarnings({"rawtypes", "unchecked"})
			public Object doInGemfire(Region region) throws GemFireCheckedException, GemFireException {
				region.putAll(map);
				return null;
			}
		});
	}

	private Map<Object, Object> parseCacheEntries(Message<?> message) {
		if (cacheEntryExpressions.size() == 0) {
			return null;
		}
		else {
			Map<Object, Object> cacheValues = new HashMap<Object, Object>();
			for (Entry<Expression, Expression> expressionEntry : cacheEntryExpressions.entrySet()) {
				cacheValues.put(expressionEntry.getKey().getValue(message), expressionEntry.getValue().getValue(message));
			}
			return cacheValues;
		}
	}

	public void setCacheEntries(Map<String, String> cacheEntries) {

		if (cacheEntryExpressions.size() > 0) {
			cacheEntryExpressions.clear();
		}

		for (Entry<String, String> cacheEntry : cacheEntries.entrySet()) {
			this.cacheEntryExpressions.put(new SpelExpressionParser().parseExpression(cacheEntry.getKey()),
					new SpelExpressionParser().parseExpression(cacheEntry.getValue()));
		}
	}
}
