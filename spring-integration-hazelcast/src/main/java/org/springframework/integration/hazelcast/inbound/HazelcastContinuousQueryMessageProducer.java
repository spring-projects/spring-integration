/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.hazelcast.inbound;

import com.hazelcast.map.IMap;
import com.hazelcast.query.impl.predicates.SqlPredicate;

import org.springframework.util.Assert;

/**
 * Hazelcast Continuous Query Message Producer is a message producer which enables
 * {@link AbstractHazelcastMessageProducer.HazelcastEntryListener} with a
 * {@link SqlPredicate} in order to listen related distributed map events in the light of
 * defined predicate and sends events to related channel.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastContinuousQueryMessageProducer extends AbstractHazelcastMessageProducer {

	private final String predicate;

	private boolean includeValue = true;

	@SuppressWarnings("rawtypes")
	public HazelcastContinuousQueryMessageProducer(IMap distributedMap, String predicate) {
		super(distributedMap);
		Assert.hasText(predicate, "'predicate' must not be null");
		this.predicate = predicate;
	}

	public void setIncludeValue(boolean includeValue) {
		this.includeValue = includeValue;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	protected void doStart() {
		setHazelcastRegisteredEventListenerId(((IMap<?, ?>) this.distributedObject)
				.addEntryListener(new HazelcastEntryListener(), new SqlPredicate(this.predicate),
						this.includeValue));
	}

	@Override
	protected void doStop() {
		((IMap<?, ?>) this.distributedObject).removeEntryListener(getHazelcastRegisteredEventListenerId());
	}

	@Override
	public String getComponentType() {
		return "hazelcast:cq-inbound-channel-adapter";
	}

}
