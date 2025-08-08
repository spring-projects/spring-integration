/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.mongodb.store;

import org.junit.jupiter.api.Test;

import org.springframework.integration.store.MessageStore;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 */
class MongoDbMessageGroupStoreTests extends AbstractMongoDbMessageGroupStoreTests {

	@Override
	protected MongoDbMessageStore getMessageGroupStore() {
		MongoDbMessageStore mongoDbMessageStore =
				new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Override
	protected MessageStore getMessageStore() {
		return getMessageGroupStore();
	}

	@Test
	void testWithAggregatorWithShutdown() {
		super.testWithAggregatorWithShutdown("mongo-aggregator-config.xml");
	}

}
