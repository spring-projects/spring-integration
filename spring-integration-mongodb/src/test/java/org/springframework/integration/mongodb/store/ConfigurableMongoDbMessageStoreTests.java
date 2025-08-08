/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mongodb.store;

import org.springframework.integration.store.MessageStore;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 */
public class ConfigurableMongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.setApplicationContext(this.testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

}
