/*
 * Copyright 2002-2016 the original author or authors.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StopWatch;

import com.mongodb.DBObject;
import com.mongodb.MongoClient;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 *
 */
public class ConfigurableMongoDbMessageGroupStoreTests extends AbstractMongoDbMessageGroupStoreTests {

	@Override
	protected ConfigurableMongoDbMessageStore getMessageGroupStore() throws Exception {
		MongoDbFactory mongoDbFactory = new SimpleMongoDbFactory(new MongoClient(), "test");
		ConfigurableMongoDbMessageStore mongoDbMessageStore = new ConfigurableMongoDbMessageStore(mongoDbFactory);
		GenericApplicationContext testApplicationContext = TestUtils.createTestApplicationContext();
		testApplicationContext.refresh();
		mongoDbMessageStore.setApplicationContext(testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Override
	protected MessageStore getMessageStore() throws Exception {
		return this.getMessageGroupStore();
	}

	@Test
	@MongoDbAvailable
	public void testWithAggregatorWithShutdown() throws Exception {
		super.testWithAggregatorWithShutdown("mongo-aggregator-configurable-config.xml");
	}

	@Test
	@Ignore("The performance test. Enough slow. Also needs the release strategy changed to size() == 1000")
	@MongoDbAvailable
	public void messageGroupStoreLazyLoadPerformance() throws Exception {
		cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));

		StopWatch watch = new StopWatch("Lazy-Load Performance");

		int sequenceSize = 1000;

		performLazyLoadEagerTest(watch, sequenceSize, true);

		performLazyLoadEagerTest(watch, sequenceSize, false);

//		System. out .println(watch.prettyPrint()); // checkstyle
	}

	private void performLazyLoadEagerTest(StopWatch watch, int sequenceSize, boolean lazyLoad) {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("mongo-aggregator-configurable-config.xml", getClass());
		context.refresh();

		AbstractMessageGroupStore messageGroupStore = context.getBean("mongoStore", AbstractMessageGroupStore.class);
		messageGroupStore.setLazyLoadMessageGroups(lazyLoad);
		MessageChannel input = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel output = context.getBean("outputChannel", QueueChannel.class);

		watch.start(lazyLoad ? "Lazy-Load" : "Eager");

		for (int i = 0; i < sequenceSize; i++) {
			input.send(MessageBuilder.withPayload("" + i)
					.setCorrelationId(1)
					.build());
		}

		assertNotNull(output.receive(20000));

		watch.stop();

		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithCustomConverter() throws Exception {
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ConfigurableMongoDbMessageStore-CustomConverter.xml", this.getClass());
		context.refresh();

		TestGateway gateway = context.getBean(TestGateway.class);
		String result = gateway.service("foo");
		assertEquals("FOO", result);
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testPriorityChannel() throws Exception {
		this.cleanupCollections(new SimpleMongoDbFactory(new MongoClient(), "test"));
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ConfigurableMongoDbMessageStore-CustomConverter.xml", this.getClass());
		context.refresh();

		Object priorityChannel = context.getBean("priorityChannel");
		assertThat(priorityChannel, Matchers.not(Matchers.instanceOf(PriorityChannel.class)));
		assertThat(priorityChannel, Matchers.instanceOf(QueueChannel.class));

		QueueChannel channel = (QueueChannel) priorityChannel;

		Message<String> message = MessageBuilder.withPayload("1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 1).build();
		channel.send(message);
		message = MessageBuilder.withPayload("-1").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, -1).build();
		channel.send(message);
		message = MessageBuilder.withPayload("3").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		channel.send(message);
		message = MessageBuilder.withPayload("0").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 0).build();
		channel.send(message);
		message = MessageBuilder.withPayload("2").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 2).build();
		channel.send(message);
		message = MessageBuilder.withPayload("none").build();
		channel.send(message);
		message = MessageBuilder.withPayload("31").setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 3).build();
		channel.send(message);

		Message<?> receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("3", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("31", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("2", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("1", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("0", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("-1", receive.getPayload());

		receive = channel.receive(1000);
		assertNotNull(receive);
		assertEquals("none", receive.getPayload());

		context.close();
	}



	public interface TestGateway {

		String service(String payload);

	}

	public static class MessageReadConverter implements Converter<DBObject, Message<?>> {

		@Override
		@SuppressWarnings("unchecked")
		public Message<?> convert(DBObject source) {
			return MessageBuilder.withPayload(source.get("payload")).copyHeaders((Map<String, ?>) source.get("headers")).build();
		}

	}

}
