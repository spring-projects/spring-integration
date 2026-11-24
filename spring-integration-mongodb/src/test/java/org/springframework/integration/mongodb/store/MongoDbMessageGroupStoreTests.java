/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
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
