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
package org.springframework.integration.mongodb.store;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.integration.test.util.TestUtils.getPropertyValue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;

import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.Message;
import org.springframework.integration.store.MessageStore;

import com.mongodb.Mongo;

/**
 * @author Amol Nayak
 *
 */
public class ConfigurableMongoDbMessageStoreTests extends AbstractMongoDbMessageStoreTests {

	@Override
	protected MessageStore getMessageStore() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new Mongo(), "test");
		return new ConfigurableMongoDbMessageStore(mongoDbFactory);
	}


	@Test
	public void testWithNullSerializer() throws Exception {
		ConfigurableMongoDbMessageStore store = (ConfigurableMongoDbMessageStore)this.getMessageStore();
		try {
			store.setSerializer(null);
		} catch (IllegalArgumentException e) {
			assertEquals("serializer is null", e.getMessage());
			return;
		}
		fail("Expected IllegalArgumentException");

	}

	@Test
	public void testWithNullDeserializer() throws Exception {
		ConfigurableMongoDbMessageStore store = (ConfigurableMongoDbMessageStore)this.getMessageStore();
		try {
			store.setDeserializer(null);
		} catch (Exception e) {
			assertEquals("deserializer is null", e.getMessage());
			return;
		}
		fail("Expected IllegalArgumentException");
	}

	@Test
	public void testWithCustomSerializer() throws Exception {
		ConfigurableMongoDbMessageStore store = (ConfigurableMongoDbMessageStore)this.getMessageStore();
		Serializer<? extends Message<?>> serializer = new Serializer<Message<?>>() {

			@Override
			public void serialize(Message<?> object, OutputStream outputStream)
					throws IOException {

			}
		};
		store.setSerializer(serializer);
		@SuppressWarnings("rawtypes")
		Serializer setSerializer = getPropertyValue(store, "serializer.serializer", Serializer.class);
		assertEquals(serializer, setSerializer);
	}

	@Test
	public void testWithNullMongoTemplate() throws Exception {
		ConfigurableMongoDbMessageStore store = (ConfigurableMongoDbMessageStore)this.getMessageStore();
		try {
			store.setMongoTemplate(null);
		} catch (IllegalArgumentException e) {
			assertEquals("mongoTemplate is null", e.getMessage());
			return;
		}
		fail("Expected IllegalArgumentException");
	}

	@Test
	public void testWithNoMongoTemplate() throws Exception {
		ConfigurableMongoDbMessageStore store = new ConfigurableMongoDbMessageStore();
		try {
			store.afterPropertiesSet();
		} catch (IllegalArgumentException e) {
			assertEquals("MongoTemplate instance cannot be null", e.getMessage());
			return;
		}
		fail("Expected IllegalArgumentException");
	}

	@Test
	public void testWithCustomDeserializer() throws Exception {
		ConfigurableMongoDbMessageStore store = (ConfigurableMongoDbMessageStore)this.getMessageStore();
		Deserializer<? extends Message<?>> deserializer = new Deserializer<Message<?>>() {

			@Override
			public Message<?> deserialize(InputStream inputStream)
					throws IOException {
				return null;
			}
		};
		store.setDeserializer(deserializer);
		@SuppressWarnings("rawtypes")
		Deserializer setDeserializer = getPropertyValue(store, "deserializer.deserializer", Deserializer.class);
		assertEquals(deserializer, setDeserializer);
	}
}
