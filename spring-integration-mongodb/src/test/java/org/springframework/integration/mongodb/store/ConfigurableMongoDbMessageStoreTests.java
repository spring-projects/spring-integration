/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mongodb.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.integration.store.MessageStore;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 * @author Glenn Renfro
 */
public class ConfigurableMongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.addAllowedPatterns("org.springframework.integration.mongodb.store.*");
		mongoDbMessageStore.setApplicationContext(this.testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = "  ")
	void addAllowedPatternsRejectsBlankPattern(String pattern) {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> mongoDbMessageStore.addAllowedPatterns(pattern));
	}

	@Test
	void addAllowedPatternsRejectsNullArray() {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> mongoDbMessageStore.addAllowedPatterns((String[]) null));
	}

}
