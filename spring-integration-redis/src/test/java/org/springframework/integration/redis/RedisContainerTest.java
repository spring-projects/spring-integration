/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.redis;

import java.time.Duration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The base contract for all tests requiring a Redis connection.
 * The Testcontainers 'reuse' option must be disabled, so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Redis container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Vozhdayenko
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 6.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface RedisContainerTest {

	GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>("redis:7.0.2")
			.withExposedPorts(6379);

	@BeforeAll
	static void startContainer() {
		REDIS_CONTAINER.start();
	}

	/**
	 * A primary method which should be used to connect to the test Redis instance.
	 * Can be used in any JUnit lifecycle methods if a test class implements this interface.
	 */
	static LettuceConnectionFactory connectionFactory() {
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setPort(REDIS_CONTAINER.getFirstMappedPort());

		LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
				.clientOptions(
						ClientOptions.builder()
								.socketOptions(
										SocketOptions.builder()
												.connectTimeout(Duration.ofMillis(10000))
												.keepAlive(true)
												.build())
								.build())
				.commandTimeout(Duration.ofSeconds(10000))
				.build();

		var connectionFactory = new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfiguration);
		connectionFactory.afterPropertiesSet();
		return connectionFactory;
	}

	static void awaitContainerSubscribed(RedisMessageListenerContainer container) throws Exception {
		awaitContainerSubscribedNoWait(container);
	}

	static void awaitContainerSubscribedNoWait(RedisMessageListenerContainer container) throws InterruptedException {
		RedisConnection connection = null;

		int n = 0;
		while (n++ < 300 &&
				(connection =
						TestUtils.<RedisConnection>getPropertyValue(container, "subscriber.connection"))
						== null) {

			Thread.sleep(100);
		}
		assertThat(connection).as("RedisMessageListenerContainer Failed to Connect").isNotNull();

		n = 0;
		while (n++ < 300 && !connection.isSubscribed()) {
			Thread.sleep(100);
		}
		assertThat(n < 300).as("RedisMessageListenerContainer Failed to Subscribe").isTrue();
	}

	static void awaitContainerSubscribedWithPatterns(RedisMessageListenerContainer container) throws Exception {
		awaitContainerSubscribed(container);
		RedisConnection connection = TestUtils.getPropertyValue(container, "subscriber.connection");

		int n = 0;
		while (n++ < 300 && connection.getSubscription().getPatterns().size() == 0) {
			Thread.sleep(100);
		}
		assertThat(n < 300).as("RedisMessageListenerContainer Failed to Subscribe with patterns").isTrue();
		// wait another second because of race condition
		Thread.sleep(1000);
	}

	static void awaitFullySubscribed(RedisMessageListenerContainer container, RedisTemplate<?, ?> redisTemplate,
			String redisChannelName, QueueChannel channel, Object message) throws Exception {
		awaitContainerSubscribedNoWait(container);
		drain(channel);
		long now = System.currentTimeMillis();
		Message<?> received = null;
		while (received == null && System.currentTimeMillis() - now < 30000) {
			redisTemplate.convertAndSend(redisChannelName, message);
			received = channel.receive(1000);
		}
		drain(channel);
		assertThat(received).as("Container failed to fully start").isNotNull();
	}

	static void drain(QueueChannel channel) {
		while (channel.receive(0) != null) {
			// drain
		}
	}

	static void prepareList(RedisConnectionFactory connectionFactory) {

		StringRedisTemplate redisTemplate = createStringRedisTemplate(connectionFactory);
		redisTemplate.delete("presidents");
		BoundListOperations<String, String> ops = redisTemplate.boundListOps("presidents");

		ops.rightPush("John Adams");

		ops.rightPush("Barack Obama");
		ops.rightPush("Thomas Jefferson");
		ops.rightPush("John Quincy Adams");
		ops.rightPush("Zachary Taylor");

		ops.rightPush("Theodore Roosevelt");
		ops.rightPush("Woodrow Wilson");
		ops.rightPush("George W. Bush");
		ops.rightPush("Franklin D. Roosevelt");
		ops.rightPush("Ronald Reagan");
		ops.rightPush("William J. Clinton");
		ops.rightPush("Abraham Lincoln");
		ops.rightPush("George Washington");
	}

	static void prepareZset(RedisConnectionFactory connectionFactory) {

		StringRedisTemplate redisTemplate = createStringRedisTemplate(connectionFactory);

		redisTemplate.delete("presidents");
		BoundZSetOperations<String, String> ops = redisTemplate.boundZSetOps("presidents");

		ops.add("John Adams", 18);

		ops.add("Barack Obama", 21);
		ops.add("Thomas Jefferson", 19);
		ops.add("John Quincy Adams", 19);
		ops.add("Zachary Taylor", 19);

		ops.add("Theodore Roosevelt", 20);
		ops.add("Woodrow Wilson", 20);
		ops.add("George W. Bush", 21);
		ops.add("Franklin D. Roosevelt", 20);
		ops.add("Ronald Reagan", 20);
		ops.add("William J. Clinton", 20);
		ops.add("Abraham Lincoln", 19);
		ops.add("George Washington", 18);
	}

	static void deletePresidents(RedisConnectionFactory connectionFactory) {
		deleteKey(connectionFactory, "presidents");
	}

	static void deleteKey(RedisConnectionFactory connectionFactory, String key) {
		StringRedisTemplate redisTemplate = createStringRedisTemplate(connectionFactory);
		redisTemplate.delete(key);
	}

	static StringRedisTemplate createStringRedisTemplate(RedisConnectionFactory connectionFactory) {
		StringRedisTemplate redisTemplate = new StringRedisTemplate();
		redisTemplate.setConnectionFactory(connectionFactory);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}

}
