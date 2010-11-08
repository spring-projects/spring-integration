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

package org.springframework.integration.twitter.inbound;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.CollectionUtils;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 */
public class DirectMessageReceivingMessageSourceTests {

	private Tweet firstMessage;

	private Tweet secondMessage;
	
	private Tweet thirdMessage;

	private Tweet fourthMessage;

	private TwitterOperations twitter;
	
	Twitter tw;


	@Before
	public void prepare() throws Exception{
		twitter = mock(TwitterOperations.class);
		firstMessage = mock(Tweet.class);
		when(firstMessage.getCreatedAt()).thenReturn(new Date(5555555555L));
		when(firstMessage.getId()).thenReturn((long) 200);
		secondMessage = mock(Tweet.class);
		when(secondMessage.getCreatedAt()).thenReturn(new Date(2222222222L));
		when(secondMessage.getId()).thenReturn((long) 2000);
		
		thirdMessage = mock(Tweet.class);
		when(thirdMessage.getCreatedAt()).thenReturn(new Date(66666666666L));
		when(thirdMessage.getId()).thenReturn((long) 3000);
		
		fourthMessage = mock(Tweet.class);
		when(fourthMessage.getCreatedAt()).thenReturn(new Date(77777777777L));
		when(fourthMessage.getId()).thenReturn((long) 4000);
		
		when(twitter.getProfileId()).thenReturn("kermit");
		Twitter tw = mock(Twitter.class);
		when(twitter.getUnderlyingTwitter()).thenReturn(tw);
	}


	@Test
	public void testSuccessfullInitialization() throws Exception{
		
		DirectMessageReceivingMessageSource source = new DirectMessageReceivingMessageSource(twitter);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setTaskScheduler(scheduler);
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		source.start();
		assertEquals("twitter:inbound-dm-channel-adapter.twitterEndpoint.kermit", TestUtils.getPropertyValue(source, "metadataKey"));
		assertTrue(source.isRunning());
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void testSuccessfullInitializationWithMessages() throws Exception{
		this.setUpMockScenarioForMessagePolling();
		
		DirectMessageReceivingMessageSource source = new DirectMessageReceivingMessageSource(twitter);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setTaskScheduler(scheduler);
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		source.start();
		Thread.sleep(1000);
		System.out.println("Tweets: " + TestUtils.getPropertyValue(source, "tweets"));
		Queue msg = (Queue) TestUtils.getPropertyValue(source, "tweets");
		assertTrue(!CollectionUtils.isEmpty(msg));	
		assertEquals(1, msg.size()); // because the other message has a older timestamp and is assumed to be read by
		Tweet message = (Tweet) msg.poll();
		assertEquals(secondMessage, message);
		Thread.sleep(1000);
		verify(twitter, times(1)).getDirectMessages(2000);
		// based on the Mock, the Queue shoud now have 2 mopre messages third and fourth
		assertTrue(((Queue)TestUtils.getPropertyValue(source, "tweets")).size() == 2);
	}


	@SuppressWarnings("unchecked")
	private void setUpMockScenarioForMessagePolling() throws Exception{
		RateLimitStatus rateLimitStatus = mock(RateLimitStatus.class);
		Twitter tw = mock(Twitter.class);
		when(twitter.getUnderlyingTwitter()).thenReturn(tw);
		when(tw.getRateLimitStatus()).thenReturn(rateLimitStatus);
		when(rateLimitStatus.getSecondsUntilReset()).thenReturn(1000);
		when(rateLimitStatus.getRemainingHits()).thenReturn(1000);

		SampleResoponceList testMessages = new SampleResoponceList();
		testMessages.add(firstMessage);
		testMessages.add(secondMessage);
		when(twitter.getDirectMessages()).thenReturn(testMessages);
		
		testMessages = new SampleResoponceList();
		testMessages.add(thirdMessage);
		testMessages.add(fourthMessage);
		when(twitter.getDirectMessages((long)2000)).thenReturn(testMessages);
	}

	@SuppressWarnings({ "rawtypes", "serial" })
	public static class SampleResoponceList extends ArrayList implements ResponseList {

		@Override
		public RateLimitStatus getRateLimitStatus() {
			return mock(RateLimitStatus.class);
		}

		@Override
		public RateLimitStatus getFeatureSpecificRateLimitStatus() {
			return mock(RateLimitStatus.class);
		}
		
	}
}
