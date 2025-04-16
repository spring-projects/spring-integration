/*
 * Copyright 2015-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.hazelcast.outbound;

import java.util.Collection;
import java.util.Map;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.topic.ITopic;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.hazelcast.HazelcastHeaders;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * MessageHandler implementation that writes {@link Message} or payload to defined
 * Hazelcast distributed cache object.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class HazelcastCacheWritingMessageHandler extends AbstractMessageHandler {

	private DistributedObject distributedObject;

	private Expression cacheExpression;

	private Expression keyExpression;

	private boolean extractPayload = true;

	private EvaluationContext evaluationContext;

	public void setDistributedObject(DistributedObject distributedObject) {
		Assert.notNull(distributedObject, "'distributedObject' must not be null");
		this.distributedObject = distributedObject;
	}

	public void setCacheExpression(Expression cacheExpression) {
		Assert.notNull(cacheExpression, "'cacheExpression' must not be null");
		this.cacheExpression = cacheExpression;
	}

	public void setKeyExpression(Expression keyExpression) {
		Assert.notNull(keyExpression, "'keyExpression' must not be null");
		this.keyExpression = keyExpression;
	}

	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected void handleMessageInternal(final Message<?> message) {
		Object objectToStore = message;
		if (this.extractPayload) {
			objectToStore = message.getPayload();
		}

		DistributedObject object = getDistributedObject(message);

		if (object instanceof Map map) {
			if (objectToStore instanceof Map) {
				map.putAll((Map) objectToStore);
			}
			else if (objectToStore instanceof Map.Entry entry) {
				map.put(entry.getKey(), entry.getValue());
			}
			else {
				map.put(getKey(message), objectToStore);
			}
		}
		else if (object instanceof MultiMap map) {
			if (objectToStore instanceof Map) {
				Map<?, ?> mapToStore = (Map) objectToStore;
				for (Map.Entry entry : mapToStore.entrySet()) {
					map.put(entry.getKey(), entry.getValue());
				}
			}
			else if (objectToStore instanceof Map.Entry entry) {
				map.put(entry.getKey(), entry.getValue());
			}
			else {
				map.put(getKey(message), objectToStore);
			}
		}
		else if (object instanceof ITopic) {
			((ITopic) object).publish(objectToStore);
		}
		else if (object instanceof Collection) {
			if (objectToStore instanceof Collection) {
				((Collection) object).addAll((Collection) objectToStore);
			}
			else {
				((Collection) object).add(objectToStore);
			}
		}
		else {
			throw new IllegalStateException("The 'object' for 'HazelcastCacheWritingMessageHandler' " +
					"must be of 'IMap', 'MultiMap', 'ITopic', 'ISet' or 'IList' type, " +
					"but gotten: [" + object + "].");
		}
	}

	private DistributedObject getDistributedObject(final Message<?> message) {
		if (this.distributedObject != null) {
			return this.distributedObject;
		}
		else if (this.cacheExpression != null) {
			return this.cacheExpression.getValue(this.evaluationContext, message, DistributedObject.class);
		}
		else if (message.getHeaders().containsKey(HazelcastHeaders.CACHE_NAME)) {
			return getBeanFactory()
					.getBean(message.getHeaders().get(HazelcastHeaders.CACHE_NAME, String.class), // NOSONAR
							DistributedObject.class);
		}
		else {
			throw new IllegalStateException("One of 'cache', 'cache-expression' and "
					+ HazelcastHeaders.CACHE_NAME
					+ " must be set for cache object definition.");
		}
	}

	private Object getKey(Message<?> message) {
		if (this.keyExpression != null) {
			return this.keyExpression.getValue(this.evaluationContext, message);
		}
		else {
			throw new IllegalStateException(
					"'key-expression' must be set to place the raw 'payload' to the IMap, MultiMap and ReplicatedMap");
		}
	}

}
