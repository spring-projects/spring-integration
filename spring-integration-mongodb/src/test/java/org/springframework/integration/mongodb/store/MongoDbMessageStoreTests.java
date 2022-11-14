/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.integration.store.MessageStore;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 */
class MongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Test
	void testCustomConverter() throws InterruptedException {
		MongoDbMessageStore mongoDbMessageStore = new MongoDbMessageStore(MONGO_DATABASE_FACTORY);
		FooToBytesConverter fooToBytesConverter = new FooToBytesConverter();
		mongoDbMessageStore.setCustomConverters(fooToBytesConverter);
		mongoDbMessageStore.afterPropertiesSet();

		mongoDbMessageStore.addMessage(new GenericMessage<>(new Foo("foo")));

		assertThat(fooToBytesConverter.called.await(10, TimeUnit.SECONDS)).isTrue();
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
