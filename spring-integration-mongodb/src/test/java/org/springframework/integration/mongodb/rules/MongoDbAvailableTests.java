/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.mongodb.rules;

import java.time.Duration;

import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.junit.Rule;

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

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;


/**
 * Convenience base class that enables unit test methods to rely upon the {@link MongoDbAvailable} annotation.
 *
 * @author Oleg Zhurakousky
 * @author Xavier Padro
 * @author Artem Bilan
 * @author David Turanski
 *
 * @since 2.1
 */
public abstract class MongoDbAvailableTests {

	@Rule
	public MongoDbAvailableRule mongoDbAvailableRule = new MongoDbAvailableRule();

	public static final MongoDatabaseFactory MONGO_DATABASE_FACTORY =
			new SimpleMongoClientDatabaseFactory(
					MongoClients.create(
							MongoClientSettings.builder().uuidRepresentation(UuidRepresentation.STANDARD).build()),
					"test");

	public static final ReactiveMongoDatabaseFactory REACTIVE_MONGO_DATABASE_FACTORY =
			new SimpleReactiveMongoDatabaseFactory(
					com.mongodb.reactivestreams.client.MongoClients.create(
							MongoClientSettings.builder().uuidRepresentation(UuidRepresentation.STANDARD).build()),
					"test");

	protected MongoDatabaseFactory prepareMongoFactory(String... additionalCollectionsToDrop) {
		cleanupCollections(MONGO_DATABASE_FACTORY, additionalCollectionsToDrop);
		return MONGO_DATABASE_FACTORY;
	}

	protected ReactiveMongoDatabaseFactory prepareReactiveMongoFactory(String... additionalCollectionsToDrop) {
		cleanupCollections(REACTIVE_MONGO_DATABASE_FACTORY, additionalCollectionsToDrop);
		return REACTIVE_MONGO_DATABASE_FACTORY;
	}

	protected void cleanupCollections(ReactiveMongoDatabaseFactory mongoDbFactory,
			String... additionalCollectionsToDrop) {

		ReactiveMongoTemplate template = new ReactiveMongoTemplate(mongoDbFactory);
		template.dropCollection("messages").block(Duration.ofSeconds(3));
		template.dropCollection("configurableStoreMessages").block(Duration.ofSeconds(3));
		template.dropCollection("data").block(Duration.ofSeconds(3));
		for (String additionalCollection : additionalCollectionsToDrop) {
			template.dropCollection(additionalCollection).block(Duration.ofSeconds(3));
		}
	}

	protected void cleanupCollections(MongoDatabaseFactory mongoDbFactory, String... additionalCollectionsToDrop) {
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		template.dropCollection("messages");
		template.dropCollection("configurableStoreMessages");
		template.dropCollection("data");
		for (String additionalCollection : additionalCollectionsToDrop) {
			template.dropCollection(additionalCollection);
		}
	}

	protected Person createPerson() {
		Address address = new Address();
		address.setCity("Philadelphia");
		address.setStreet("2121 Rawn street");
		address.setState("PA");

		Person person = new Person();
		person.setAddress(address);
		person.setName("Oleg");
		return person;
	}

	protected Person createPerson(String name) {
		Address address = new Address();
		address.setCity("Philadelphia");
		address.setStreet("2121 Rawn street");
		address.setState("PA");

		Person person = new Person();
		person.setAddress(address);
		person.setName(name);
		return person;
	}

	public static class Person {

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

	public static class Address {

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

	public static class TestMongoConverter extends MappingMongoConverter {

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

	public static class ReactiveTestMongoConverter extends MappingMongoConverter {

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

	public static class TestCollectionCallback implements MessageCollectionCallback<Long> {

		@Override
		public Long doInCollection(MongoCollection<Document> collection, Message<?> message)
				throws MongoException, DataAccessException {

			return collection.countDocuments();
		}

	}

}
