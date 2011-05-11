/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.twitter.core;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.test.util.TestUtils;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.http.AccessToken;
import twitter4j.http.Authorization;
import twitter4j.http.OAuthAuthorization;

/**
 * Validates that all calls are delegated properly top Twitter
 * 
 * @author Oleg Zhurakousky
 *
 */
public class Twitter4jTemplateTests {
	Twitter4jTemplate template;
	Twitter twitter;
	
	@Before
	public void prepare() throws Exception{
		template = new Twitter4jTemplate();
		Field twitterField = Twitter4jTemplate.class.getDeclaredField("twitter");
		twitterField.setAccessible(true);
		twitter = mock(Twitter.class);
		twitterField.set(template, twitter);
	}
	@Test
	public void testOauthConstructor() throws Exception{
		template = new Twitter4jTemplate("a", "b", "1234-c", "d");
		Twitter twitter = (Twitter) TestUtils.getPropertyValue(template, "twitter");
		Authorization auth = twitter.getAuthorization();
		assertTrue(twitter.getAuthorization() instanceof OAuthAuthorization);
		AccessToken accessToken = ((OAuthAuthorization)auth).getOAuthAccessToken();
		assertEquals("1234-c", accessToken.getToken());
		assertEquals("d", accessToken.getTokenSecret());
	}

	@Test
	public void testProfileId() throws Exception{
		when(twitter.getScreenName()).thenReturn("kermit");
		when(twitter.isOAuthEnabled()).thenReturn(true);
		assertEquals("kermit", template.getProfileId());
	}
	
	@Test
	public void testGetDirectMessages() throws Exception{
		template.getDirectMessages();
		template.getDirectMessages(123);
		verify(twitter, times(1)).getDirectMessages();
		verify(twitter, times(1)).getDirectMessages(Mockito.any(Paging.class));
	}
	
	@Test
	public void testGetMentions() throws Exception{
		template.getMentions();
		template.getMentions(123);
		verify(twitter, times(1)).getMentions();
		verify(twitter, times(1)).getMentions(Mockito.any(Paging.class));
	}
	
	@Test
	public void testGetFriendsTimeline() throws Exception{
		template.getTimeline();
		template.getTimeline(123);
		verify(twitter, times(1)).getHomeTimeline();
		verify(twitter, times(1)).getHomeTimeline(Mockito.any(Paging.class));
	}
	
	@Test
	public void testSendDirectMessage() throws Exception{
		template.sendDirectMessage("kermit", "hello");
		template.sendDirectMessage(1, "hello");
		verify(twitter, times(1)).sendDirectMessage("kermit", "hello");
		verify(twitter, times(1)).sendDirectMessage(1, "hello");
	}
	
	@Test
	public void testUpdateStatus() throws Exception{
		template.updateStatus("writing twitter test");
		verify(twitter, times(1)).updateStatus(Mockito.any(StatusUpdate.class));
	}
	
	@Test
	public void testSearch() throws Exception{
		// set up test
		QueryResult result = mock(QueryResult.class);
		List<twitter4j.Tweet> t4jTweets = new ArrayList<twitter4j.Tweet>();
		t4jTweets.add(mock(twitter4j.Tweet.class));
		t4jTweets.add(mock(twitter4j.Tweet.class));
		t4jTweets.add(mock(twitter4j.Tweet.class));
		
		when(result.getTweets()).thenReturn(t4jTweets);
		
		when(twitter.search(Mockito.any(Query.class))).thenReturn(result);
		// end setup test
		
		SearchResults results = template.search("#s2gx");
		List<Tweet> tweets = results.getTweets();
		assertNotNull(tweets);
		assertEquals(3, tweets.size());
	}
	
}
