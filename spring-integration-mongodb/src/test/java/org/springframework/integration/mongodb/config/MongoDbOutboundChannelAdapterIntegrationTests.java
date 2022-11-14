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

package org.springframework.integration.mongodb.config;

import com.mongodb.BasicDBObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.integration.mongodb.MongoDbContainerTest;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 */
class MongoDbOutboundChannelAdapterIntegrationTests implements MongoDbContainerTest {

	static MongoDatabaseFactory MONGO_DATABASE_FACTORY;

	@BeforeAll
	static void prepareMongoConnection() {
		MONGO_DATABASE_FACTORY = MongoDbContainerTest.createMongoDbFactory();
	}

	@Test
	void testWithDefaultMongoFactory() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbContainerTest.Person>(MongoDbContainerTest.createPerson("Bob"));
		channel.send(message);

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);
		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data")).isNotNull();
		context.close();
	}

	@Test
	void testWithNamedCollection() throws Exception {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, "foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithNamedCollection", MessageChannel.class);
		Message<Person> message =
				MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob"))
						.setHeader("collectionName", "foo")
						.build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	void testWithTemplate() throws Exception {
		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, "foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);
		Message<Person> message =
				MessageBuilder.withPayload(MongoDbContainerTest.createPerson("Bob"))
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	void testSavingDbObject() throws Exception {

		BasicDBObject dbObject = BasicDBObject.parse("{'foo' : 'bar'}");

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, "foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);

		Message<BasicDBObject> message =
				MessageBuilder.withPayload(dbObject)
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	void testSavingJSONString() throws Exception {

		String object = "{'foo' : 'bar'}";

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY, "foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);

		Message<String> message =
				MessageBuilder.withPayload(object)
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	void testWithMongoConverter() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithConverter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbContainerTest.Person>(MongoDbContainerTest.createPerson("Bob"));
		channel.send(message);

		MongoDbContainerTest.prepareMongoData(MONGO_DATABASE_FACTORY);
		MongoTemplate template = new MongoTemplate(MONGO_DATABASE_FACTORY);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data")).isNotNull();
		context.close();
	}

}
