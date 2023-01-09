/*
 * Copyright 2002-2023 the original author or authors.
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
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.mongodb.rules.MongoDbAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class MongoDbOutboundChannelAdapterIntegrationTests extends MongoDbAvailableTests {

	@Test
	@MongoDbAvailable
	public void testWithDefaultMongoFactory() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbAvailableTests.Person>(this.createPerson("Bob"));
		channel.send(message);

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data")).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithNamedCollection() throws Exception {
		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithNamedCollection", MessageChannel.class);
		Message<Person> message =
				MessageBuilder.withPayload(this.createPerson("Bob"))
						.setHeader("collectionName", "foo")
						.build();
		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithTemplate() throws Exception {
		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);
		Message<Person> message =
				MessageBuilder.withPayload(this.createPerson("Bob"))
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testSavingDbObject() throws Exception {

		BasicDBObject dbObject = BasicDBObject.parse("{'foo' : 'bar'}");

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);

		Message<BasicDBObject> message =
				MessageBuilder.withPayload(dbObject)
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testSavingJSONString() throws Exception {

		String object = "{'foo' : 'bar'}";

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory("foo");
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithTemplate", MessageChannel.class);

		Message<String> message =
				MessageBuilder.withPayload(object)
						.setHeader("collectionName", "foo")
						.build();

		channel.send(message);

		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'foo' : 'bar'}"), BasicDBObject.class, "foo")).isNotNull();
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithMongoConverter() throws Exception {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("outbound-adapter-config.xml", this.getClass());

		MessageChannel channel = context.getBean("simpleAdapterWithConverter", MessageChannel.class);
		Message<Person> message = new GenericMessage<MongoDbAvailableTests.Person>(this.createPerson("Bob"));
		channel.send(message);

		MongoDatabaseFactory mongoDbFactory = this.prepareMongoFactory();
		MongoTemplate template = new MongoTemplate(mongoDbFactory);
		assertThat(template.find(new BasicQuery("{'name' : 'Bob'}"), Person.class, "data")).isNotNull();
		context.close();
	}

}
