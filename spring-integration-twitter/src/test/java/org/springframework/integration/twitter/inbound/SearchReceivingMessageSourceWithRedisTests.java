/*
 * Copyright 2013-2018 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.redis.metadata.RedisMetadataStore;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.test.rule.Log4j2LevelAdjuster;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.social.twitter.api.SearchMetadata;
import org.springframework.social.twitter.api.SearchOperations;
import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.UserOperations;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
@ContextConfiguration("SearchReceivingMessageSourceWithRedisTests-context.xml")
@RunWith(SpringRunner.class)
@DirtiesContext
public class SearchReceivingMessageSourceWithRedisTests extends RedisAvailableTests {

	@Rule
	public Log4j2LevelAdjuster adjuster = Log4j2LevelAdjuster.trace();

	@Autowired
	private SourcePollingChannelAdapter twitterSearchAdapter;

	@Autowired
	private AbstractTwitterMessageSource<?> twitterMessageSource;

	@Autowired
	private MetadataStore metadataStore;

	@Autowired
	@Qualifier("inbound_twitter")
	private PollableChannel tweets;

	@Test
	@RedisAvailable
	public void testPollForTweetsThreeResultsWithRedisMetadataStore() throws Exception {
		String metadataKey = TestUtils.getPropertyValue(twitterSearchAdapter, "source.metadataKey", String.class);

		// There is need to set a value, not 'remove' and re-init 'twitterMessageSource'
		this.metadataStore.put(metadataKey, "-1");

		this.twitterMessageSource.afterPropertiesSet();

		MetadataStore metadataStore = TestUtils.getPropertyValue(this.twitterSearchAdapter, "source.metadataStore",
				MetadataStore.class);
		assertTrue("Expected metadataStore to be an instance of RedisMetadataStore",
				metadataStore instanceof RedisMetadataStore);
		assertSame(this.metadataStore, metadataStore);

		assertEquals("twitterSearchAdapter.74", metadataKey);

		this.twitterSearchAdapter.start();

		Message<?> receive = this.tweets.receive(10000);
		assertNotNull(receive);

		receive = this.tweets.receive(10000);
		assertNotNull(receive);

		receive = this.tweets.receive(10000);
		assertNotNull(receive);

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

		receive = this.tweets.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(Tweet.class));
		assertEquals(((Tweet) receive.getPayload()).getId(), 2L);

		receive = this.tweets.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(Tweet.class));
		assertEquals(((Tweet) receive.getPayload()).getId(), 3L);

		assertNull(this.tweets.receive(0));

		persistedMetadataStoreValue = this.metadataStore.get(metadataKey);
		assertNotNull(persistedMetadataStoreValue);
		assertEquals("3", persistedMetadataStoreValue);
	}

	@Configuration
	public static class SearchReceivingMessageSourceWithRedisTestsConfig {

		@Bean(name = "twitterTemplate")
		public TwitterTemplate twitterTemplate() {
			TwitterTemplate twitterTemplate = mock(TwitterTemplate.class);

			SearchOperations so = mock(SearchOperations.class);

			Tweet tweet3 = mock(Tweet.class);
			given(tweet3.getId()).willReturn(3L);
			given(tweet3.getCreatedAt()).willReturn(new GregorianCalendar(2013, 2, 20).getTime());
			given(tweet3.toString()).will(invocation -> "Mock for Tweet: " + tweet3.getId());

			Tweet tweet1 = mock(Tweet.class);
			given(tweet1.getId()).willReturn(1L);
			given(tweet1.getCreatedAt()).willReturn(new GregorianCalendar(2013, 0, 20).getTime());
			given(tweet1.toString()).will(invocation -> "Mock for Tweet: " + tweet1.getId());

			final Tweet tweet2 = mock(Tweet.class);
			given(tweet2.getId()).willReturn(2L);
			given(tweet2.getCreatedAt()).willReturn(new GregorianCalendar(2013, 1, 20).getTime());
			given(tweet2.toString()).will(invocation -> "Mock for Tweet: " + tweet2.getId());

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
