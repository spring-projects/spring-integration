/*
 * Copyright 2015-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.integration.mongodb.MongoDbContainerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Senthil Arumugam, Samiraj Panneer Selvam
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 *
 * @since 4.2
 *
 */
class MongoDbMetadataStoreTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	private static final String DEFAULT_COLLECTION_NAME = "metadataStore";

	private final String file1 = "/remotepath/filesTodownload/file-1.txt";

	private final String file1Id = "12345";

	private MongoDbMetadataStore store = null;

	@BeforeEach
	public void configure() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, DEFAULT_COLLECTION_NAME);
		this.store = new MongoDbMetadataStore(MONGO_DATABASE_FACTORY);
	}

	@Test
	void testConfigureCustomCollection() {
		final String collectionName = "testMetadataStore";
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, collectionName);
		final MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		store = new MongoDbMetadataStore(template, collectionName);
		testBasics();
	}

	@Test
	void testConfigureFactory() {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, DEFAULT_COLLECTION_NAME);
		store = new MongoDbMetadataStore(MONGO_DATABASE_FACTORY);
		testBasics();
	}

	@Test
	void testConfigureFactorCustomCollection() {
		final String collectionName = "testMetadataStore";
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, collectionName);
		store = new MongoDbMetadataStore(MONGO_DATABASE_FACTORY, collectionName);
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
	void testGetFromStore() {
		testBasics();
	}

	@Test
	void testPutIfAbsent() {
		String fileID = store.get(file1);
		assertThat(fileID).as("Get First time, Value must not exist").isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).as("Insert First time, Value must return null").isNull();

		fileID = store.putIfAbsent(file1, "56789");
		assertThat(fileID)
				.as("Key Already Exists - Insertion Failed, ol value must be returned").isNotNull()
				.as("The Old Value must be equal to returned").isEqualTo(file1Id);

		assertThat(store.get(file1)).as("The Old Value must return").isEqualTo(file1Id);

	}

	@Test
	void testRemove() {
		String fileID = store.remove(file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).isNull();

		fileID = store.remove(file1);
		assertThat(fileID)
				.isNotNull()
				.isEqualTo(file1Id);

		fileID = store.get(file1);
		assertThat(fileID).isNull();
	}

	@Test
	void testReplace() {
		boolean removedValue = store.replace(file1, file1Id, "4567");
		assertThat(removedValue).isFalse();
		String fileID = store.get(file1);
		assertThat(fileID).isNull();

		fileID = store.putIfAbsent(file1, file1Id);
		assertThat(fileID).isNull();

		removedValue = store.replace(file1, file1Id, "4567");
		assertThat(removedValue).isTrue();

		fileID = store.get(file1);
		assertThat(fileID)
				.isNotNull()
				.isEqualTo("4567");
	}

}
