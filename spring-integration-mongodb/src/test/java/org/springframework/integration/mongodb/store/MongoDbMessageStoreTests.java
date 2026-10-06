/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.mongodb.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.data.convert.WritingConverter;
import org.springframework.integration.store.MessageStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 *
 */
class MongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	private static final String[] ERROR_PAYLOAD_PATTERNS = {
			"org.springframework.messaging.MessagingException",
			"org.springframework.core.NestedRuntimeException",
			"org.springframework.messaging.support.GenericMessage",
			"org.springframework.messaging.MessageHeaders",
			"java.util.UUID",
			"java.util.HashMap",
			"java.lang.RuntimeException",
			"java.lang.Exception",
			"java.lang.Throwable",
			"java.lang.StackTraceElement",
			"java.util.Collections$*",
			Person.class.getName()
	};

	private static final String[] TRUSTED_PATTERNS = {
			"java.lang.IllegalStateException",
			"java.lang.RuntimeException",
			"java.lang.Exception",
			"java.lang.Throwable",
			"java.lang.StackTraceElement",
			"java.util.Collections$*"
	};

	@Override
	protected MessageStore getMessageStore() {
		MongoDbMessageStore mongoDbMessageStore =
				new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
						MongoDbMessageStore.DEFAULT_COLLECTION_NAME, ERROR_PAYLOAD_PATTERNS);
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Test
	void testCustomConverter() throws InterruptedException {
		MongoDbMessageStore mongoDbMessageStore =
				new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
						MongoDbMessageStore.DEFAULT_COLLECTION_NAME, ERROR_PAYLOAD_PATTERNS);
		FooToBytesConverter fooToBytesConverter = new FooToBytesConverter();
		mongoDbMessageStore.setCustomConverters(fooToBytesConverter);
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();

		mongoDbMessageStore.addMessage(new GenericMessage<>(new Foo("foo")));

		assertThat(fooToBytesConverter.called.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyConstructorIsUnrestricted() {
		MongoDbMessageStore store = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		initialize(store);
		assertStoresAndReads(store, new UntrustedException());
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyAddAllowedPatternsAddsPatterns() {
		MongoDbMessageStore store = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		store.addAllowedPatterns(TRUSTED_PATTERNS);
		initialize(store);
		assertUnauthorized(store);
		assertThatIllegalArgumentException().isThrownBy(store::addAllowedPatterns);
		assertUnauthorized(store);
		store.addAllowedPatterns(UntrustedException.class.getName());
		assertStoresAndReads(store, new UntrustedException());
		assertStoresAndReads(store, new IllegalStateException("still trusted"));
	}

	@Test
	void patternsConstructorEnforcesAllowList() {
		MongoDbMessageStore store = new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
				MongoDbMessageStore.DEFAULT_COLLECTION_NAME, TRUSTED_PATTERNS);
		initialize(store);
		assertStoresAndReads(store, new IllegalStateException("trusted"));
		assertUnauthorized(store);
	}

	@Test
	void patternsConstructorRejectsEmptyCollectionName() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageStore(MONGO_DATABASE_FACTORY, " ", TRUSTED_PATTERNS))
				.withMessage("'collectionName' must not be empty");
	}

	@Test
	void patternsConstructorRejectsInvalidPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
						MongoDbMessageStore.DEFAULT_COLLECTION_NAME, (String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageStore(MONGO_DATABASE_FACTORY, "collection", new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
						MongoDbMessageStore.DEFAULT_COLLECTION_NAME, "java.lang.*", " "))
				.withMessageContaining("whitespace-only");
	}

	@Test
	void addAllowedPatternsExtendsPatternsAndRejectsInvalid() {
		MongoDbMessageStore store = new MongoDbMessageStore(MONGO_DATABASE_FACTORY,
				MongoDbMessageStore.DEFAULT_COLLECTION_NAME, TRUSTED_PATTERNS);
		initialize(store);
		assertUnauthorized(store);
		assertThatIllegalArgumentException().isThrownBy(store::addAllowedPatterns);
		assertThatIllegalArgumentException().isThrownBy(() -> store.addAllowedPatterns(""));
		assertUnauthorized(store);
		store.addAllowedPatterns(UntrustedException.class.getName());
		assertStoresAndReads(store, new UntrustedException());
	}

	private void initialize(MongoDbMessageStore store) {
		store.setApplicationContext(testApplicationContext);
		store.afterPropertiesSet();
	}

	private static void assertStoresAndReads(MongoDbMessageStore store, Throwable throwable) {
		ErrorMessage errorMessage = new ErrorMessage(throwable);
		store.addMessage(errorMessage);
		assertThat(store.getMessage(errorMessage.getHeaders().getId()))
				.extracting(Message::getPayload)
				.isInstanceOf(throwable.getClass());
	}

	private static void assertUnauthorized(MongoDbMessageStore store) {
		ErrorMessage errorMessage = new ErrorMessage(new UntrustedException());
		store.addMessage(errorMessage);
		assertThatExceptionOfType(ConversionFailedException.class)
				.isThrownBy(() -> store.getMessage(errorMessage.getHeaders().getId()))
				.havingCause()
				.isInstanceOf(SerializationFailedException.class)
				.havingCause()
				.isInstanceOf(SecurityException.class);
	}

	@SuppressWarnings("serial")
	private static final class UntrustedException extends RuntimeException {

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
