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

import javax.sql.DataSource;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.expression.Expression;
import org.springframework.integration.core.PseudoTransactionalMessageSource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
/**
 * A polling channel adapter that returns messages from Redis Sorted Sets
 * It allows you to specify the {@link #scoreRange} attribute.
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisZsetInboundChannelAdapter extends AbstractRedisCollectionInboundChannelAdapter
		implements PseudoTransactionalMessageSource<Collection<Object>, BoundZSetOperations<String, Object>> {

	private final ThreadLocal<BoundZSetOperations<String, Object>> resourceHolder = new ThreadLocal<BoundZSetOperations<String, Object>>();

	private volatile String scoreRange;

	private volatile double lowerScoreRange = Double.MIN_VALUE;

	private volatile double upperScoreRange = Double.MAX_VALUE;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained and the select query to execute to retrieve new rows.
	 *
	 * @param dataSource Must not be null
	 * @param selectQuery query to execute
	 */
	public RedisZsetInboundChannelAdapter(RedisConnectionFactory connectionFactory, Expression keyExpression) {
		super(connectionFactory, keyExpression);
	}

	public void setScoreRange(String scoreRange) {
		this.scoreRange = scoreRange;
	}


	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (StringUtils.hasText(this.scoreRange)){
			String[] lowerUpperRanges = StringUtils.split(this.scoreRange, "-");
			try {
				if (lowerUpperRanges == null){
					this.lowerScoreRange = Double.parseDouble(this.scoreRange);
					this.upperScoreRange = this.lowerScoreRange;
				}
				else {
					this.lowerScoreRange = Double.parseDouble(lowerUpperRanges[0]);
					if (lowerUpperRanges.length > 1){
						this.upperScoreRange = Double.parseDouble(lowerUpperRanges[1]);
					}
					else {
						this.upperScoreRange = this.lowerScoreRange;
					}
				}
				Assert.isTrue(this.upperScoreRange >= this.lowerScoreRange, "Upper score-range can not be lower then Lower score-range. " +
						"Upper is " + this.upperScoreRange + "; Lower is " + this.lowerScoreRange);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Failed to process scoreRange '" + this.scoreRange + "'. It may contain an invalid value." +
						" Valid values could be a single Integer or a range of Integers delimited by '-' (e.g., score-range=\"5\" or score-range=\"5-10\"", e);
			}
		}
	}

	@Override
	protected Collection<Object> doPoll(String key) {
		BoundZSetOperations<String, Object> ops = redisTemplate.boundZSetOps(key);
		resourceHolder.set(ops);
		return ops.rangeByScore(this.lowerScoreRange, this.upperScoreRange);
	}

	@Override
	public String getComponentType(){
		return "redis:zset-inbound-channel-adapter";
	}

	public BoundZSetOperations<String, Object> getResource() {
		return this.resourceHolder.get();
	}

	public void afterCommit(Object object) {
		this.resourceHolder.remove();
	}

	public void afterRollback(Object object) {
		this.resourceHolder.remove();
	}

	public void afterReceiveNoTx(BoundZSetOperations<String, Object> resource) {
		this.resourceHolder.remove();
	}

	public void afterSendNoTx(BoundZSetOperations<String, Object> resource) {
		this.resourceHolder.remove();
	}
}
