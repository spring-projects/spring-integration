/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.inbound;

import java.util.Collection;
import java.util.Collections;

import com.hazelcast.map.IMap;
import com.hazelcast.query.impl.predicates.SqlPredicate;

import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.hazelcast.DistributedSQLIterationType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Hazelcast Distributed SQL Message Source is a message source which runs defined
 * distributed query in the cluster and returns results in the light of iteration type.
 *
 * @author Eren Avsarogullari
 * @author Artem Bilan
 *
 * @since 6.0
 */
@SuppressWarnings("rawtypes")
public class HazelcastDistributedSQLMessageSource extends AbstractMessageSource {

	private final IMap<?, ?> distributedMap;

	private final String distributedSql;

	private DistributedSQLIterationType iterationType = DistributedSQLIterationType.VALUE;

	public HazelcastDistributedSQLMessageSource(IMap distributedMap, String distributedSql) {
		Assert.notNull(distributedMap, "'distributedMap' must not be null");
		Assert.hasText(distributedSql, "'distributedSql' must not be empty");
		this.distributedMap = distributedMap;
		this.distributedSql = distributedSql;
	}

	public void setIterationType(DistributedSQLIterationType iterationType) {
		Assert.notNull(this.iterationType, "'iterationType' must not be null");
		this.iterationType = iterationType;
	}

	@Override
	public String getComponentType() {
		return "hazelcast:ds-inbound-channel-adapter";
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Collection<?> doReceive() {
		final SqlPredicate predicate = new SqlPredicate(this.distributedSql);
		Collection<?> collection =
				switch (this.iterationType) {
					case ENTRY -> this.distributedMap.entrySet(predicate);
					case KEY -> this.distributedMap.keySet(predicate);
					case LOCAL_KEY -> this.distributedMap.localKeySet(predicate);
					default -> this.distributedMap.values(predicate);
				};

		if (CollectionUtils.isEmpty(collection)) {
			return null;
		}

		return Collections.unmodifiableCollection(collection);
	}

}
