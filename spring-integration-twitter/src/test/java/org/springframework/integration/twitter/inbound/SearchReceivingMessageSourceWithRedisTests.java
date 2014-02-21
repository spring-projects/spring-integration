/*
 * Copyright 2013-2014 the original author or authors.
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
import static org.junit.Assert.assertSame;
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
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.PollableChannel;
import org.springframework.social.twitter.api.SearchMetadata;
import org.springframework.social.twitter.api.SearchOperations;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.UserOperations;
import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.impl.TwitterTemplate;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 3.0
 */
public class SearchReceivingMessageSourceWithRedisTests extends RedisAvailableTests {

	private SourcePollingChannelAdapter twitterSearchAdapter;

	private AbstractTwitterMessageSource<?> twitterMessageSource;

	private MetadataStore metadataStore;

	private String metadataKey;

	private PollableChannel tweets;

	@Before
	public void setup() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(SearchReceivingMessageSourceWithRedisTestsConfig.class);
		context.registerShutdownHook();
		context.refresh();

		this.twitterSearchAdapter = context.getBean(SourcePollingChannelAdapter.class);
		this.twitterMessageSource = context.getBean(AbstractTwitterMessageSource.class);
		this.metadataStore = context.getBean(MetadataStore.class);
		this.tweets = context.getBean("inbound_twitter", PollableChannel.class);

		this.metadataKey = TestUtils.getPropertyValue(twitterSearchAdapter, "source.metadataKey", String.class);

		// There is need to set a value, not 'remove' and re-init 'twitterMessageSource'
		this.metadataStore.put(metadataKey, "-1");

		this.twitterMessageSource.afterPropertiesSet();
	}

	/**
	 * Verify that a polling operation returns in fact 3 results.
	 * @throws Exception
	 */
	@Test
	@RedisAvailable
	public void testPollForTweetsThreeResultsWithRedisMetadataStore() throws Exception {
		MetadataStore metadataStore = TestUtils.getPropertyValue(this.twitterSearchAdapter, "source.metadataStore", MetadataStore.class);
		assertTrue("Exptected metadataStore to be an instance of RedisMetadataStore", metadataStore instanceof RedisMetadataStore);
		assertSame(this.metadataStore, metadataStore);

		assertEquals("twitterSearchAdapter.74", metadataKey);

		this.twitterSearchAdapter.start();

		assertNotNull(this.tweets.receive(10000));
		assertNotNull(this.tweets.receive(1000));
		assertNotNull(this.tweets.receive(1000));

		/* We received 3 messages so far. When invoking receive() again the search
		 * will return again the 3 test Tweets but as we already processed them
		 * no message (null) is returned. */
		assertNull(this.tweets.receive(0));

		String persistedMetadataStoreValue = this.metadataStore.get(metadataKey);
		assertNotNull(persistedMetadataStoreValue);
		assertEquals("3", persistedMetadataStoreValue);

		this.twitterSearchAdapter.stop();

		this.metadataStore.put(metadataKey, "1");

		this.twitterMessageSource.afterPropertiesSet();

		this.twitterSearchAdapter.start();

		assertNotNull(this.tweets.receive(1000));
		assertNotNull(this.tweets.receive(1000));

		assertNull(this.tweets.receive(0));

		persistedMetadataStoreValue = this.metadataStore.get(metadataKey);
		assertNotNull(persistedMetadataStoreValue);
		assertEquals("3", persistedMetadataStoreValue);
	}

	@Configuration
	@ImportResource("classpath:org/springframework/integration/twitter/inbound/SearchReceivingMessageSourceWithRedisTests-context.xml")
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

			when(twitterTemplate.isAuthorized()).thenReturn(true);

			final UserOperations userOperations = mock(UserOperations.class);
			when(twitterTemplate.userOperations()).thenReturn(userOperations);
			when(userOperations.getProfileId()).thenReturn(74L);

			return twitterTemplate;
		}

	}

}
