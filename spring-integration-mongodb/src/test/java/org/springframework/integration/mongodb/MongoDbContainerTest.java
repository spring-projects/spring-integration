/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.integration.mongodb;

import java.time.Duration;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.dao.DataAccessException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.MongoPersistentProperty;
import org.springframework.integration.mongodb.outbound.MessageCollectionCallback;
import org.springframework.messaging.Message;

/**
 * The base contract for all tests requiring a MongoDb connection.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Redis container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Oleg Zhurakousky
 * @author Xavier Padro
 * @author Artem Bilan
 * @author David Turanski
 * @author Artem Vozhdayenko
 *
 * @since 6.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface MongoDbContainerTest {

	MongoDBContainer MONGO_CONTAINER = new MongoDBContainer("mongo:5.0.9");

	@BeforeAll
	static void startContainer() {
		MONGO_CONTAINER.start();
	}

	static MongoDatabaseFactory createMongoDbFactory() {
		return new SimpleMongoClientDatabaseFactory(MongoClients.create(getMongoClientSettings()), "test");
	}

	static ReactiveMongoDatabaseFactory createReactiveMongoDbFactory() {
		return new SimpleReactiveMongoDatabaseFactory(
				com.mongodb.reactivestreams.client.MongoClients.create(getMongoClientSettings()),
				"test");
	}

	private static MongoClientSettings getMongoClientSettings() {
		return MongoClientSettings.builder()
				.applyConnectionString(new ConnectionString(
						"mongodb://localhost:" + MONGO_CONTAINER.getFirstMappedPort()))
				.uuidRepresentation(UuidRepresentation.STANDARD).build();
	}

	static void prepareMongoData(MongoDatabaseFactory mongoDatabaseFactory, String... additionalCollectionsToDrop) {
		cleanupCollections(mongoDatabaseFactory, additionalCollectionsToDrop);
	}

	static void prepareReactiveMongoData(ReactiveMongoDatabaseFactory mongoDatabaseFactory, String... additionalCollectionsToDrop) {
		cleanupCollections(mongoDatabaseFactory, additionalCollectionsToDrop);
	}

	static void cleanupCollections(ReactiveMongoDatabaseFactory mongoDbFactory,
			String... additionalCollectionsToDrop) {

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(mongoDbFactory);
		template.dropCollection("messages").block(Duration.ofSeconds(3));
		template.dropCollection("configurableStoreMessages").block(Duration.ofSeconds(3));
		template.dropCollection("data").block(Duration.ofSeconds(3));
		for (String additionalCollection : additionalCollectionsToDrop) {
			template.dropCollection(additionalCollection).block(Duration.ofSeconds(3));
		}
	}

	static void cleanupCollections(MongoDatabaseFactory mongoDbFactory, String... additionalCollectionsToDrop) {
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.dropCollection("messages");
		template.dropCollection("configurableStoreMessages");
		template.dropCollection("data");
		for (String additionalCollection : additionalCollectionsToDrop) {
			template.dropCollection(additionalCollection);
		}
	}

	static Person createPerson() {
		Address address = new Address();
		address.setCity("Philadelphia");
		address.setStreet("2121 Rawn street");
		address.setState("PA");

		Person person = new Person();
		person.setAddress(address);
		person.setName("Oleg");
		return person;
	}

	static Person createPerson(String name) {
		Address address = new Address();
		address.setCity("Philadelphia");
		address.setStreet("2121 Rawn street");
		address.setState("PA");

		Person person = new Person();
		person.setAddress(address);
		person.setName(name);
		return person;
	}

	class Person {

		@Id
		private String id;

		private Address address;

		private String name;

		public Address getAddress() {
			return address;
		}

		public void setAddress(Address address) {
			this.address = address;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	class Address {

		private String street;

		private String city;

		private String state;

		public String getStreet() {
			return street;
		}

		public void setStreet(String street) {
			this.street = street;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}

		public String getState() {
			return state;
		}

		public void setState(String state) {
			this.state = state;
		}

	}

	class TestMongoConverter extends MappingMongoConverter {

		public TestMongoConverter(
				MongoDatabaseFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {

			super(new DefaultDbRefResolver(mongoDbFactory), mappingContext);
		}

		@Override
		public void write(Object source, Bson target) {
			super.write(source, target);
		}

		@Override
		public <S> S read(Class<S> clazz, Bson source) {
			return super.read(clazz, source);
		}

	}

	class ReactiveTestMongoConverter extends MappingMongoConverter {

		public ReactiveTestMongoConverter(
				ReactiveMongoDatabaseFactory mongoDbFactory,
				MappingContext<? extends MongoPersistentEntity<?>, MongoPersistentProperty> mappingContext) {

			super(NoOpDbRefResolver.INSTANCE, mappingContext);
		}

		@Override
		public void write(Object source, Bson target) {
			super.write(source, target);
		}

		@Override
		public <S> S read(Class<S> clazz, Bson source) {
			return super.read(clazz, source);
		}

	}

	class TestCollectionCallback implements MessageCollectionCallback<Long> {

		@Override
		public Long doInCollection(MongoCollection<Document> collection, Message<?> message)
				throws MongoException, DataAccessException {

			return collection.countDocuments();
		}

	}

}
