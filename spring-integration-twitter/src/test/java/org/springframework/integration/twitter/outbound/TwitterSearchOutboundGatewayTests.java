/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.twitter.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.integration.twitter.outbound.TwitterSearchOutboundGatewayTests.TwitterConfig;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.social.twitter.api.SearchMetadata;
import org.springframework.social.twitter.api.SearchOperations;
import org.springframework.social.twitter.api.SearchParameters;
import org.springframework.social.twitter.api.SearchResults;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration(classes=TwitterConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class TwitterSearchOutboundGatewayTests {

	@Autowired
	private SearchOperations searchOps;

	@Autowired
	private TwitterSearchOutboundGateway gateway;

	@Autowired
	private PollableChannel outputChannel;

	@Test
	public void testStringQuery() {
		Tweet tweet = new Tweet(1L, "foo", new Date(), "bar", "baz", 0L, 0L, "qux", "fiz");
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		final SearchResults searchResults = new SearchResults(Collections.singletonList(tweet), searchMetadata);
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertEquals("foo", searchParameters.getQuery());
				assertEquals(Integer.valueOf(20), searchParameters.getCount());
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<String>("foo"));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(1, tweets.size());
		assertSame(tweet, tweets.get(0));
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Test
	public void testStringQueryCustomLimit() {
		this.gateway.setSearchArgsExpression(new SpelExpressionParser()
				.parseExpression("{payload, 30}"));
		Tweet tweet = new Tweet(1L, "foo", new Date(), "bar", "baz", 0L, 0L, "qux", "fiz");
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		final SearchResults searchResults = new SearchResults(Collections.singletonList(tweet), searchMetadata);
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertEquals("foo", searchParameters.getQuery());
				assertEquals(Integer.valueOf(30), searchParameters.getCount());
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<String>("foo"));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(1, tweets.size());
		assertSame(tweet, tweets.get(0));
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Test
	public void testStringQueryCustomExpression() {
		this.gateway.setSearchArgsExpression(new SpelExpressionParser()
				.parseExpression("{'bar', 1, 2, 3}"));
		Tweet tweet = new Tweet(1L, "foo", new Date(), "bar", "baz", 0L, 0L, "qux", "fiz");
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		final SearchResults searchResults = new SearchResults(Collections.singletonList(tweet), searchMetadata);
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertEquals("bar", searchParameters.getQuery());
				assertEquals(Integer.valueOf(1), searchParameters.getCount());
				assertEquals(Long.valueOf(2), searchParameters.getSinceId());
				assertEquals(Long.valueOf(3), searchParameters.getMaxId());
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<String>("foo"));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(1, tweets.size());
		assertSame(tweet, tweets.get(0));
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Test
	public void testSearchParamsQuery() {
		Tweet tweet = new Tweet(1L, "foo", new Date(), "bar", "baz", 0L, 0L, "qux", "fiz");
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		final SearchResults searchResults = new SearchResults(Collections.singletonList(tweet), searchMetadata);
		final SearchParameters parameters = new SearchParameters("bar");
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertSame(parameters, searchParameters);
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<SearchParameters>(parameters));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(1, tweets.size());
		assertSame(tweet, tweets.get(0));
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Test
	public void testSearchParamsQueryCustomExpression() {
		this.gateway.setSearchArgsExpression(new SpelExpressionParser()
				.parseExpression("new SearchParameters('foo' + payload).count(5).sinceId(11)"));
		Tweet tweet = new Tweet(1L, "foo", new Date(), "bar", "baz", 0L, 0L, "qux", "fiz");
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		final SearchResults searchResults = new SearchResults(Collections.singletonList(tweet), searchMetadata);
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertEquals("foobar", searchParameters.getQuery());
				assertEquals(Integer.valueOf(5), searchParameters.getCount());
				assertEquals(Long.valueOf(11), searchParameters.getSinceId());
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<String>("bar"));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(1, tweets.size());
		assertSame(tweet, tweets.get(0));
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Test
	public void testEmptyResult() {
		SearchMetadata searchMetadata = mock(SearchMetadata.class);
		List<Tweet> empty = new ArrayList<Tweet>(0);
		final SearchResults searchResults = new SearchResults(empty, searchMetadata);
		doAnswer(new Answer<SearchResults>() {

			@Override
			public SearchResults answer(InvocationOnMock invocation) throws Throwable {
				SearchParameters searchParameters = (SearchParameters) invocation.getArguments()[0];
				assertEquals("foo", searchParameters.getQuery());
				assertEquals(Integer.valueOf(20), searchParameters.getCount());
				return searchResults;
			}
		}).when(this.searchOps).search(Matchers.any(SearchParameters.class));
		this.gateway.handleMessage(new GenericMessage<String>("foo"));
		Message<?> reply = this.outputChannel.receive(0);
		assertNotNull(reply);
		@SuppressWarnings("unchecked")
		List<Tweet> tweets = (List<Tweet>) reply.getPayload();
		assertEquals(0, tweets.size());
		assertSame(searchMetadata, reply.getHeaders().get(TwitterHeaders.SEARCH_METADATA));
	}

	@Configuration
	@EnableIntegration
	public static class TwitterConfig {

		@Bean
		public TwitterSearchOutboundGateway gateway() {
			TwitterSearchOutboundGateway gateway = new TwitterSearchOutboundGateway(twitter());
			gateway.setOutputChannel(outputChannel());
			return gateway;
		}

		@Bean
		public PollableChannel outputChannel() {
			return new QueueChannel();
		}

		@Bean
		public Twitter twitter() {
			Twitter twitter = mock(Twitter.class);
			when(twitter.searchOperations()).thenReturn(searchOps());
			return twitter;
		}

		@Bean
		public SearchOperations searchOps() {
			return mock(SearchOperations.class);
		}

	}

}
