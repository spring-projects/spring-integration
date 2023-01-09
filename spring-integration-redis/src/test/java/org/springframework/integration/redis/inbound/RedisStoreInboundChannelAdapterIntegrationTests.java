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

package org.springframework.integration.redis.inbound;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.RedisZSet;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.messaging.Message;
import org.springframework.messaging.SubscribableChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.2
 */
public class RedisStoreInboundChannelAdapterIntegrationTests extends RedisAvailableTests {

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testListInboundConfiguration() throws Exception {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca = context.getBean("listAdapter", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<Integer> message = (Message<Integer>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(Integer.valueOf(13));

		//poll again, should get the same stuff
		message = (Message<Integer>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(Integer.valueOf(13));
		this.deletePresidents(jcf);
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	// synchronization commit renames the list
	public void testListInboundConfigurationWithSynchronization() throws Exception {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("bar");
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca =
				context.getBean("listAdapterWithSynchronization", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<Integer> message = (Message<Integer>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(Integer.valueOf(13));

		//poll again, should get nothing since the collection was removed during synchronization
		message = (Message<Integer>) redisChannel.receive(100);
		assertThat(message).isNull();

		int n = 0;
		while (n++ < 100 && template.keys("bar").size() == 0) {
			Thread.sleep(100);
		}
		assertThat(n < 100).as("Rename didn't occur").isTrue();

		assertThat(template.boundListOps("bar").size()).isEqualTo(Long.valueOf(13));
		template.delete("bar");

		spca.stop();
		context.close();
	}

	@SuppressWarnings("resource")
	@Test
	@RedisAvailable
	// synchronization rollback renames the list
	public void testListInboundConfigurationWithSynchronizationAndRollback() throws Exception {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("baz");
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("list-inbound-adapter.xml",
				this.getClass());
		SubscribableChannel fail = context.getBean("redisFailChannel", SubscribableChannel.class);
		final CountDownLatch latch = new CountDownLatch(1);
		fail.subscribe(message -> {
			latch.countDown();
			throw new RuntimeException("Test Rollback");
		});
		SourcePollingChannelAdapter spca = context.getBean("listAdapterWithSynchronizationAndRollback",
				SourcePollingChannelAdapter.class);
		spca.start();
		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		int n = 0;
		while (n++ < 100 && template.keys("baz").size() == 0) {
			Thread.sleep(100);
		}
		assertThat(n < 100).as("Rename didn't occur").isTrue();
		assertThat(template.boundListOps("baz").size()).isEqualTo(Long.valueOf(13));
		template.delete("baz");

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	// synchronization commit renames the list
	public void testListInboundConfigurationWithSynchronizationAndTemplate() throws Exception {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		StringRedisTemplate template = this.createStringRedisTemplate(jcf);
		template.delete("bar");
		this.prepareList(jcf);
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("list-inbound-adapter.xml", this.getClass());
		SourcePollingChannelAdapter spca =
				context.getBean("listAdapterWithSynchronizationAndRedisTemplate", SourcePollingChannelAdapter.class);
		spca.start();
		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<Integer> message = (Message<Integer>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(Integer.valueOf(13));

		//poll again, should get nothing since the collection was removed during synchronization
		message = (Message<Integer>) redisChannel.receive(100);
		assertThat(message).isNull();

		int n = 0;
		while (n++ < 100 && template.keys("bar").size() == 0) {
			Thread.sleep(100);
		}
		assertThat(n < 100).as("Rename didn't occur").isTrue();

		assertThat(template.boundListOps("bar").size()).isEqualTo(Long.valueOf(13));
		template.delete("bar");

		spca.stop();
		context.close();
	}

	@Test
	@RedisAvailable
	@SuppressWarnings("unchecked")
	public void testZsetInboundAdapter() throws InterruptedException {
		RedisConnectionFactory jcf = this.getConnectionFactoryForTest();
		this.prepareZset(jcf);
		ClassPathXmlApplicationContext context =
				new ClassPathXmlApplicationContext("zset-inbound-adapter.xml", this.getClass());

		//No Score test
		SourcePollingChannelAdapter zsetAdapterNoScore =
				context.getBean("zsetAdapterNoScore", SourcePollingChannelAdapter.class);
		zsetAdapterNoScore.start();

		QueueChannel redisChannel = context.getBean("redisChannel", QueueChannel.class);

		Message<RedisZSet<Object>> message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().size()).isEqualTo(13);

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().size()).isEqualTo(13);

		zsetAdapterNoScore.stop();


		//ScoreRange test
		SourcePollingChannelAdapter zsetAdapterWithScoreRange =
				context.getBean("zsetAdapterWithScoreRange", SourcePollingChannelAdapter.class);
		zsetAdapterWithScoreRange.start();

		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().rangeByScore(18, 20).size()).isEqualTo(11);

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().rangeByScore(18, 20).size()).isEqualTo(11);

		zsetAdapterWithScoreRange.stop();


		//SingleScore test
		SourcePollingChannelAdapter zsetAdapterWithSingleScore =
				context.getBean("zsetAdapterWithSingleScore", SourcePollingChannelAdapter.class);
		zsetAdapterWithSingleScore.start();

		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().rangeByScore(18, 18).size()).isEqualTo(2);

		//poll again, should get the same stuff
		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().rangeByScore(18, 18).size()).isEqualTo(2);

		zsetAdapterWithSingleScore.stop();


		//SingleScoreAndSynchronization test
		SourcePollingChannelAdapter zsetAdapterWithSingleScoreAndSynchronization =
				context.getBean("zsetAdapterWithSingleScoreAndSynchronization", SourcePollingChannelAdapter.class);

		QueueChannel otherRedisChannel = context.getBean("otherRedisChannel", QueueChannel.class);

		// get all 13 presidents
		zsetAdapterNoScore.start();
		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload().size()).isEqualTo(13);
		zsetAdapterNoScore.stop();

		// get only presidents for 18th century
		zsetAdapterWithSingleScoreAndSynchronization.start();
		Message<Integer> sizeMessage = (Message<Integer>) otherRedisChannel.receive(10000);
		assertThat(sizeMessage).isNotNull();
		assertThat(sizeMessage.getPayload()).isEqualTo(Integer.valueOf(2));

		// ... however other elements are still available 13-2=11
		zsetAdapterNoScore.start();
		message = (Message<RedisZSet<Object>>) redisChannel.receive(10000);
		assertThat(message).isNotNull();

		int n = 0;
		while (n++ < 100 && message.getPayload().size() != 11) {
			Thread.sleep(100);
		}
		assertThat(n < 100).isTrue();

		zsetAdapterNoScore.stop();
		zsetAdapterWithSingleScoreAndSynchronization.stop();
		this.deletePresidents(jcf);

		context.close();
	}

}
