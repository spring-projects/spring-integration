/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mongodb.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 */
class MongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.addAllowedPatterns("org.springframework.integration.mongodb.store.*");
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Test
	void addAllowedPatternsAccumulates() {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.addAllowedPatterns("com.example.model.*");
		mongoDbMessageStore.addAllowedPatterns("com.example.other.*");

		AllowListDeserializingConverter deserializingConverter =
				TestUtils.getPropertyValue(mongoDbMessageStore, "errorPayloadDeserializingConverter");
		assertThat(deserializingConverter.getAllowedPatterns())
				.containsAll(AllowListDeserializingConverter.DEFAULT_ALLOWED_PATTERNS)
				.contains("com.example.model.*", "com.example.other.*");
	}

	@Test
	void testCustomConverter() throws InterruptedException {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		FooToBytesConverter fooToBytesConverter = new FooToBytesConverter();
		mongoDbMessageStore.setCustomConverters(fooToBytesConverter);
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();

		mongoDbMessageStore.addMessage(new GenericMessage<>(new Foo("foo")));

		assertThat(fooToBytesConverter.called.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	void allowedPatternsAddedAfterInitializationAreApplied() {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		mongoDbMessageStore.addAllowedPatterns("com.example.trusted.*");

		ErrorMessage errorMessage = new ErrorMessage(new StoreFailureException("untrusted failure"));
		mongoDbMessageStore.addMessage(errorMessage);

		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> mongoDbMessageStore.getMessage(errorMessage.getHeaders().getId()))
				.havingRootCause()
				.isInstanceOf(SecurityException.class)
				.withMessageContaining("Attempt to deserialize unauthorized");
	}

	@Test
	void matchingAllowedPatternsAddedAfterInitializationPermitDeserialization() {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		mongoDbMessageStore.addAllowedPatterns("org.springframework.integration.mongodb.store.*");

		ErrorMessage errorMessage = new ErrorMessage(new StoreFailureException("trusted failure"));
		mongoDbMessageStore.addMessage(errorMessage);

		assertThat(mongoDbMessageStore.getMessage(errorMessage.getHeaders().getId()))
				.isInstanceOf(ErrorMessage.class)
				.extracting(Message::getPayload)
				.asInstanceOf(InstanceOfAssertFactories.THROWABLE)
				.isInstanceOf(StoreFailureException.class)
				.hasMessage("trusted failure");
	}

	private static class StoreFailureException extends RuntimeException {

		StoreFailureException(String message) {
			super(message);
		}

	}

	private static class Foo {

		String foo;

		Foo(String foo) {
			this.foo = foo;
		}

		@Override
		public String toString() {
			return foo;
		}

	}

	@WritingConverter
	private static class FooToBytesConverter implements Converter<Foo, byte[]> {

		private final CountDownLatch called = new CountDownLatch(1);

		@Override
		public byte[] convert(Foo source) {
			try {
				return source.toString().getBytes();
			}
			finally {
				this.called.countDown();
			}
		}

	}

}
