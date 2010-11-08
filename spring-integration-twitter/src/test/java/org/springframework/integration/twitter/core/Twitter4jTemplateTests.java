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
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.test.util.TestUtils;

import twitter4j.Paging;
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
		template = new Twitter4jTemplate("a", "b", "c", "d");
		Twitter twitter = (Twitter) TestUtils.getPropertyValue(template, "twitter");
		Authorization auth = twitter.getAuthorization();
		assertTrue(twitter.getAuthorization() instanceof OAuthAuthorization);
		AccessToken accessToken = ((OAuthAuthorization)auth).getOAuthAccessToken();
		assertEquals("c", accessToken.getToken());
		assertEquals("d", accessToken.getTokenSecret());
	}

	@Test
	public void testProfileId() throws Exception{
		when(twitter.getScreenName()).thenReturn("kermit");
		assertEquals("kermit", template.getProfileId());
	}
	@Test
	public void testRateLimitStatus() throws Exception{
		template.getRateLimitStatus();
		verify(twitter, times(1)).getRateLimitStatus();
	}
	
	@Test
	public void testGetDirectMessages() throws Exception{
		template.getDirectMessages();
		template.getDirectMessages(new Paging());
		verify(twitter, times(1)).getDirectMessages();
		verify(twitter, times(1)).getDirectMessages(Mockito.any(Paging.class));
	}
	
	@Test
	public void testGetMentions() throws Exception{
		template.getMentions();
		template.getMentions(new Paging());
		verify(twitter, times(1)).getMentions();
		verify(twitter, times(1)).getMentions(Mockito.any(Paging.class));
	}
	
	@Test
	public void testGetFriendsTimeline() throws Exception{
		template.getFriendsTimeline();
		template.getFriendsTimeline(new Paging());
		verify(twitter, times(1)).getFriendsTimeline();
		verify(twitter, times(1)).getFriendsTimeline(Mockito.any(Paging.class));
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
		StatusUpdate status = new StatusUpdate("writing twitter test");
		template.updateStatus(status);
		verify(twitter, times(1)).updateStatus(Mockito.any(StatusUpdate.class));
	}
}
