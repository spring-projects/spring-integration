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

package org.springframework.integration.mongodb.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.bson.Document;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.PriorityChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mongodb.rules.MongoDbAvailable;
import org.springframework.integration.store.AbstractMessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.StopWatch;

/**
 * @author Amol Nayak
 * @author Artem Bilan
 *
 */
public class ConfigurableMongoDbMessageGroupStoreTests extends AbstractMongoDbMessageGroupStoreTests {

	@Override
	protected ConfigurableMongoDbMessageStore getMessageGroupStore() {
		ConfigurableMongoDbMessageStore mongoDbMessageStore =
				new ConfigurableMongoDbMessageStore(MONGO_DATABASE_FACTORY);
		mongoDbMessageStore.setApplicationContext(this.testApplicationContext);
		mongoDbMessageStore.afterPropertiesSet();
		return mongoDbMessageStore;
	}

	@Override
	protected MessageStore getMessageStore() {
		return getMessageGroupStore();
	}

	@Test
	@MongoDbAvailable
	public void testWithAggregatorWithShutdown() {
		super.testWithAggregatorWithShutdown("mongo-aggregator-configurable-config.xml");
	}

	@Test
	@Ignore("The performance test. Enough slow. Also needs the release strategy changed to size() == 1000")
	@MongoDbAvailable
	public void messageGroupStoreLazyLoadPerformance() {
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

		assertThat(output.receive(20000)).isNotNull();

		watch.stop();

		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testWithCustomConverter() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ConfigurableMongoDbMessageStore-CustomConverter.xml", this
						.getClass());
		context.refresh();

		TestGateway gateway = context.getBean(TestGateway.class);
		String result = gateway.service("foo");
		assertThat(result).isEqualTo("FOO");
		context.close();
	}

	@Test
	@MongoDbAvailable
	public void testPriorityChannel() {
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("ConfigurableMongoDbMessageStore-CustomConverter.xml", this
						.getClass());
		context.refresh();

		Object priorityChannel = context.getBean("priorityChannel");
		assertThat(priorityChannel).isInstanceOf(PriorityChannel.class);

		QueueChannel channel = (QueueChannel) priorityChannel;

		Message<String> message =
				MessageBuilder.withPayload("1")
						.setHeader(IntegrationMessageHeaderAccessor.PRIORITY, 1)
						.build();
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
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("3");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("31");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("2");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("1");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("0");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("-1");

		receive = channel.receive(1000);
		assertThat(receive).isNotNull();
		assertThat(receive.getPayload()).isEqualTo("none");

		context.close();
	}


	public interface TestGateway {

		String service(String payload);

	}

	@ReadingConverter
	public static class MessageReadConverter implements Converter<Document, GenericMessage<?>> {

		@Override
		@SuppressWarnings("unchecked")
		public GenericMessage<?> convert(Document source) {
			return new GenericMessage<>(source.get("payload"), (Map<String, Object>) source.get("headers"));
		}

	}

}
