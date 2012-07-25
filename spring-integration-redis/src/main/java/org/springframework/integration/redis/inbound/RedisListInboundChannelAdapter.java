/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.redis.inbound;

import java.util.Collection;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.expression.Expression;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
/**
 * A polling channel adapter that returns messages from Redis List
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisListInboundChannelAdapter extends AbstractRedisCollectionInboundChannelAdapter
		implements PseudoTransactionalMessageSource<Collection<Object>, BoundListOperations<String, Object>> {

	private final ThreadLocal<BoundListOperations<String, Object>> resourceHolder = new ThreadLocal<BoundListOperations<String, Object>>();

	public RedisListInboundChannelAdapter(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		super(connectionFactory, keyExpression);
	}

	@Override
	protected Collection<Object> doPoll(String key) {
		BoundListOperations<String, Object> ops = redisTemplate.boundListOps(key);
		resourceHolder.set(ops);
		return ops.range(Long.MIN_VALUE, Long.MAX_VALUE);
	}
	@Override
	public String getComponentType(){
		return "redis:list-inbound-channel-adapter";
	}

	public BoundListOperations<String, Object> getResource() {
		return this.resourceHolder.get();
	}

	public void afterCommit(Object object) {
		this.resourceHolder.remove();
	}

	public void afterRollback(Object object) {
		this.resourceHolder.remove();
	}

	public void afterReceiveNoTx(BoundListOperations<String, Object> resource) {
		this.resourceHolder.remove();
	}

	public void afterSendNoTx(BoundListOperations<String, Object> resource) {
		this.resourceHolder.remove();
	}
}
