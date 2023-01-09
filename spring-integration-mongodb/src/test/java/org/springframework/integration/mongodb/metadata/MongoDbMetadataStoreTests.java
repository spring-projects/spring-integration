/*
 * Copyright 2015-2023 the original author or authors.
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

package org.springframework.integration.mongodb.metadata;

import org.junit.Before;
import org.junit.Test;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Senthil Arumugam, Samiraj Panneer Selvam
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class MongoDbMetadataStoreTests extends MongoDbAvailableTests {

	private static final String DEFAULT_COLLECTION_NAME = "metadataStore";

	private final String file1 = "/remotepath/filesTodownload/file-1.txt";

	private final String file1Id = "12345";

	private MongoDbMetadataStore store = null;

	@Before
	public void configure() {
		final MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory(DEFAULT_COLLECTION_NAME);
		this.store = new MongoDbMetadataStore(mongoDbFactory);
	}

	@MongoDbAvailable
	@Test
	public void testConfigureCustomCollection() {
		final String collectionName = "testMetadataStore";
		final MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory(collectionName);
		final MongoTemplate template = new MongoTemplate(mongoDbFactory);
		store = new MongoDbMetadataStore(template, collectionName);
		testBasics();
	}

	@MongoDbAvailable
	@Test
	public void testConfigureFactory() {
		final MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory(DEFAULT_COLLECTION_NAME);
		store = new MongoDbMetadataStore(mongoDbFactory);
		testBasics();
	}

	@MongoDbAvailable
	@Test
	public void testConfigureFactorCustomCollection() {
		final String collectionName = "testMetadataStore";
		final MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory(collectionName);
		store = new MongoDbMetadataStore(mongoDbFactory, collectionName);
		testBasics();
	}

	private void testBasics() {
		String fileID = store.get(file1);
		assertThat(fileID).isNull();

		store.put(file1, file1Id);

		fileID = store.get(file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo(file1Id);
	}

	@Test
	@MongoDbAvailable
	public void testGetFromStore() {
		testBasics();
	}

	@Test
	@MongoDbAvailable
	public void testPutIfAbsent() {
		String fileID = store.get(file1);
		assertThat(fileID).as("Get First time, Value must not exist").isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).as("Insert First time, Value must return null").isNull();

		fileID = store.putIfAbsent(file1, "56789");
		assertThat(fileID).as("Key Already Exists - Insertion Failed, ol value must be returned").isNotNull();
		assertThat(fileID).as("The Old Value must be equal to returned").isEqualTo(file1Id);

		assertThat(store.get(file1)).as("The Old Value must return").isEqualTo(file1Id);

	}

	@Test
	@MongoDbAvailable
	public void testRemove() {
		String fileID = store.remove(file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).isNull();

		fileID = store.remove(file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo(file1Id);

		fileID = store.get(file1);
		assertThat(fileID).isNull();
	}

	@Test
	@MongoDbAvailable
	public void testReplace() {
		boolean removedValue = store.replace(file1, file1Id, "4567");
		assertThat(removedValue).isFalse();
		String fileID = store.get(file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).isNull();

		removedValue = store.replace(file1, file1Id, "4567");
		assertThat(removedValue).isTrue();

		fileID = store.get(file1);
		assertThat(fileID).isNotNull();
		assertThat(fileID).isEqualTo("4567");
	}

}
