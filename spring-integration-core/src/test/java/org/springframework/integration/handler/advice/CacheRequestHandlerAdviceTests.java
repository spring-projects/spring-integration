/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.integration.handler.advice;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.CacheEvictOperation;
import org.springframework.cache.interceptor.CachePutOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.annotation.Transformer;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.2
 */
@SpringJUnitConfig
@DirtiesContext
public class CacheRequestHandlerAdviceTests {

	private static final String TEST_CACHE = "testCache";

	private static final String TEST_PUT_CACHE = "testPutCache";

	@Autowired
	private AtomicInteger cachedMethodCounter;

	@Autowired
	private MessageChannel transformerChannel;

	@Autowired
	private MessageChannel serviceChannel;

	@Autowired
	private CacheManager cacheManager;

	@Test
	@SuppressWarnings("unchecked")
	void testCacheRequestHandlerAdvice() {
		GenericMessage<String> testMessage1 = new GenericMessage<>("foo");
		this.transformerChannel.send(testMessage1);
		GenericMessage<String> testMessage2 = new GenericMessage<>("foo");
		this.transformerChannel.send(testMessage2);
		this.transformerChannel.send(new GenericMessage<>("foo"));

		assertThat(this.cachedMethodCounter.get()).isEqualTo(1);
		Cache testCache = cacheManager.getCache(TEST_CACHE);
		assertThat(testCache).isNotNull();

		ConcurrentMap<Object, Object> nativeCache = (ConcurrentMap<Object, Object>) testCache.getNativeCache();
		assertThat(nativeCache).hasSize(1);
		assertThat(nativeCache.values()).element(0).isSameAs(testMessage1);

		this.serviceChannel.send(testMessage1);
		this.serviceChannel.send(testMessage2);

		assertThat(nativeCache).hasSize(0);

		testCache = cacheManager.getCache(TEST_PUT_CACHE);
		assertThat(testCache).isNotNull();

		nativeCache = (ConcurrentMap<Object, Object>) testCache.getNativeCache();
		assertThat(nativeCache).hasSize(1);
		assertThat(nativeCache.values()).element(0).isSameAs(testMessage2);
	}

	@Configuration
	@EnableIntegration
	public static class Config {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager();
		}

		@Bean
		public CacheRequestHandlerAdvice cacheAdvice() {
			CacheRequestHandlerAdvice cacheRequestHandlerAdvice = new CacheRequestHandlerAdvice(TEST_CACHE);
			cacheRequestHandlerAdvice.setKeyExpressionString("payload");
			return cacheRequestHandlerAdvice;
		}

		@Bean
		public AtomicInteger cachedMethodCounter() {
			return new AtomicInteger();
		}

		@Transformer(inputChannel = "transformerChannel", outputChannel = "nullChannel", adviceChain = "cacheAdvice")
		public Object transform(Message<?> message) {
			cachedMethodCounter().getAndIncrement();
			return MessageBuilder.fromMessage(message);
		}

		@Bean
		public CacheRequestHandlerAdvice cachePutAndEvictAdvice() {
			CacheRequestHandlerAdvice cacheRequestHandlerAdvice = new CacheRequestHandlerAdvice();
			cacheRequestHandlerAdvice.setKeyExpressionString("payload");
			CachePutOperation.Builder cachePutBuilder = new CachePutOperation.Builder();
			cachePutBuilder.setCacheName(TEST_PUT_CACHE);
			CacheEvictOperation.Builder cacheEvictBuilder = new CacheEvictOperation.Builder();
			cacheEvictBuilder.setCacheName(TEST_CACHE);
			cacheRequestHandlerAdvice.setCacheOperations(cachePutBuilder.build(), cacheEvictBuilder.build());
			return cacheRequestHandlerAdvice;
		}

		@ServiceActivator(inputChannel = "serviceChannel", outputChannel = "nullChannel",
				adviceChain = "cachePutAndEvictAdvice")
		public Message<?> service(Message<?> message) {
			return message;
		}

	}

}
