/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.twitter.inbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.redis.store.metadata.RedisMetadataStore;
import org.springframework.integration.store.metadata.MetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.social.twitter.api.SearchMetadata;
import org.springframework.social.twitter.api.SearchOperations;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.impl.SearchParameters;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

/**
 * @author Gunnar Hillert
 */
public class SearchReceivingMessageSourceWithRedisTests extends RedisAvailableTests {

	private SourcePollingChannelAdapter twitterSearchAdapter;
	private RedisConnectionFactory redisConnectionFactory;
	private StringRedisTemplate redisTemplate;

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void setup() {
		context.register(SearchReceivingMessageSourceWithRedisTestsConfig.class);
		context.registerShutdownHook();
		context.refresh();

		this.redisConnectionFactory = context.getBean(RedisConnectionFactory.class);
		this.twitterSearchAdapter = context.getBean(SourcePollingChannelAdapter.class);
		this.redisTemplate = new StringRedisTemplate(redisConnectionFactory);
	}

	/**
	 * Verify that a polling operation returns in fact 3 results.
	 * @throws Exception
	 */
	@Test
	@RedisAvailable
	public void testPollForTweetsThreeResultsWithRedisMetadataStore() throws Exception {

		final MetadataStore metadataStore = TestUtils.getPropertyValue(twitterSearchAdapter, "source.metadataStore", MetadataStore.class);
		assertTrue("Exptected metadataStore to be an instance of RedisMetadataStore", metadataStore instanceof RedisMetadataStore);

		/*
		 * The metadataKey is automatically generated. To ensure that we use the
		 * the correct key, we retrieve it from the adapter.
		 */
		final String metadataKey = TestUtils.getPropertyValue(twitterSearchAdapter, "source.metadataKey", String.class);

		/*
		 * As we had to retrieve the metadataKey from the adapter. The metdataStore
		 * was already invoked and the id retrieved from Redis before we had a chance
		 * to reset possibly pre-existing values.
		 *
		 * Rather than deleting the value, we have to set a value, because "null" values
		 * returned from the MetadataStore are ignored by the onInit() method in
		 * the AbstractTwitterMessageSource. */
		redisTemplate.opsForValue().set(metadataKey, "-1");
		assertEquals("-1", redisTemplate.opsForValue().get(metadataKey));

		final SearchReceivingMessageSource source = TestUtils.getPropertyValue(twitterSearchAdapter, "source", SearchReceivingMessageSource.class);

		/* We need to call onInit() in order to update the id from the metadataStore. */
		source.onInit();

		final Message<?> message1 = source.receive();
		final Message<?> message2 = source.receive();
		final Message<?> message3 = source.receive();

		/* We received 3 messages so far. When invoking receive() again the search
		 * will return again the 3 test Tweets but as we already processed them
		 * no message (null) is returned. */
		final Message<?> message4 = source.receive();

		assertNotNull(message1);
		assertNotNull(message2);
		assertNotNull(message3);
		assertNull(message4);

		final String persistedMetadataStoreValue = redisTemplate.opsForValue().get(metadataKey);
		assertNotNull(persistedMetadataStoreValue);
		assertEquals("3", redisTemplate.opsForValue().get(metadataKey));

		redisTemplate.delete(metadataKey);
	}

	@Configuration
	@ImportResource("classpath:**/SearchReceivingMessageSourceWithRedisTests-context.xml")
	static class SearchReceivingMessageSourceWithRedisTestsConfig {

		@Bean(name="twitterTemplate")
		public TwitterTemplate twitterTemplate() {
			final TwitterTemplate twitterTemplate = mock(TwitterTemplate.class);

			final SearchOperations so = mock(SearchOperations.class);

			final Tweet tweet3 = new Tweet(3L, "first", new GregorianCalendar(2013, 2, 20).getTime(), "fromUser", "profileImageUrl", 888L, 999L, "languageCode", "source");
			final Tweet tweet1 = new Tweet(1L, "first", new GregorianCalendar(2013, 0, 20).getTime(), "fromUser", "profileImageUrl", 888L, 999L, "languageCode", "source");
			final Tweet tweet2 = new Tweet(2L, "first", new GregorianCalendar(2013, 1, 20).getTime(), "fromUser", "profileImageUrl", 888L, 999L, "languageCode", "source");

			final List<Tweet> tweets = new ArrayList<Tweet>();

			tweets.add(tweet3);
			tweets.add(tweet1);
			tweets.add(tweet2);

			final SearchResults results = new SearchResults(tweets, new SearchMetadata(111, 111));

			when(twitterTemplate.searchOperations()).thenReturn(so);
			when(twitterTemplate.searchOperations().search(any(SearchParameters.class))).thenReturn(results);

			return twitterTemplate;
		}
	}
}
