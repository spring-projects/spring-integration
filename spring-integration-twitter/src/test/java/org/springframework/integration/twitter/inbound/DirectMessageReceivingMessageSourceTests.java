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
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.store.PropertiesPersistingMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.Twitter4jTemplate;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.ResponseList;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 */
public class DirectMessageReceivingMessageSourceTests {

	private DirectMessage firstMessage;

	private DirectMessage secondMessage;
	
	private DirectMessage thirdMessage;

	private DirectMessage fourthMessage;

	private TwitterOperations twitter;
	
	Twitter tw;


	@Before
	public void prepare() throws Exception{
		twitter = new Twitter4jTemplate();
		
		firstMessage = mock(DirectMessage.class);
		when(firstMessage.getCreatedAt()).thenReturn(new Date(5555555555L));
		when(firstMessage.getId()).thenReturn( (long) 200);
		
		secondMessage = mock(DirectMessage.class);
		when(secondMessage.getCreatedAt()).thenReturn(new Date(2222222222L));
		when(secondMessage.getId()).thenReturn( (long) 2000);
		
		thirdMessage = mock(DirectMessage.class);
		when(thirdMessage.getCreatedAt()).thenReturn(new Date(66666666666L));
		when(thirdMessage.getId()).thenReturn( (long) 3000);
		
		fourthMessage = mock(DirectMessage.class);
		when(fourthMessage.getCreatedAt()).thenReturn(new Date(77777777777L));
		when(fourthMessage.getId()).thenReturn( (long) 4000);
		
		tw = mock(Twitter.class);
		Field twField = Twitter4jTemplate.class.getDeclaredField("twitter");
		twField.setAccessible(true);
		twField.set(twitter, tw);
		when(tw.getScreenName()).thenReturn("kermit");
		
		twitter = spy(twitter);
	}


	@Test
	public void testSuccessfullInitialization() throws Exception{
		when(tw.isOAuthEnabled()).thenReturn(true);
		DirectMessageReceivingMessageSource source = new DirectMessageReceivingMessageSource(twitter);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		assertEquals("twitter:dm-inbound-channel-adapter.twitterEndpoint.kermit", TestUtils.getPropertyValue(source, "metadataKey"));
	}
	
	@SuppressWarnings({ "unchecked" })
	@Test
	public void testSuccessfullInitializationWithMessages() throws Exception{
		this.setUpMockScenarioForMessagePolling();
		
		DirectMessageReceivingMessageSource source = new DirectMessageReceivingMessageSource(twitter);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		Message<Tweet> msg = (Message<Tweet>) source.receive();
		assertNotNull(msg);
		
		Tweet message = msg.getPayload();
		assertEquals(2000, message.getId());
		Thread.sleep(1000);
		verify(twitter, times(1)).getDirectMessages();
	}
	/**
	 * This test will validate that last status is initialized from the metadatastore
	 * @throws Exception
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void testSuccessfullInitializationWithMessagesWithPersistentMetadata() throws Exception{
		String fileName = System.getProperty("java.io.tmpdir") + "/spring-integration/metadata-store.properties";
		File file = new File(fileName);
		if (file.exists()){
			file.delete();
		}
		this.setUpMockScenarioForMessagePolling();
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		PropertiesPersistingMetadataStore store = new PropertiesPersistingMetadataStore();
		store.afterPropertiesSet();
		bf.registerSingleton(IntegrationContextUtils.METADATA_STORE_BEAN_NAME, store);
		DirectMessageReceivingMessageSource source = new DirectMessageReceivingMessageSource(twitter);
		source.setBeanFactory(bf);
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		Message msg = source.receive();
		assertNotNull(msg);
		Tweet tweet = (Tweet) msg.getPayload();

		
		// Resuming
		this.prepare();
		this.setUpMockScenarioForMessagePolling();
		store.destroy();
		bf = new DefaultListableBeanFactory();
		store = new PropertiesPersistingMetadataStore();
		store.afterPropertiesSet();
		bf.registerSingleton(IntegrationContextUtils.METADATA_STORE_BEAN_NAME, store);	
		source = new DirectMessageReceivingMessageSource(twitter);
		source.setBeanFactory(bf);
		scheduler = new ThreadPoolTaskScheduler();
		scheduler.afterPropertiesSet();
		source.setBeanName("twitterEndpoint");
		source.afterPropertiesSet();
		msg = source.receive();
		tweet = (Tweet) msg.getPayload();
		assertEquals(3000, tweet.getId());
		msg = source.receive();
		tweet = (Tweet) msg.getPayload();
		assertEquals(4000, tweet.getId());
		file.delete();
	}


	@SuppressWarnings("unchecked")
	private void setUpMockScenarioForMessagePolling() throws Exception{
		RateLimitStatus rateLimitStatus = mock(RateLimitStatus.class);
		when(tw.isOAuthEnabled()).thenReturn(true);
		when(tw.getRateLimitStatus()).thenReturn(rateLimitStatus);
		when(rateLimitStatus.getSecondsUntilReset()).thenReturn(1000);
		when(rateLimitStatus.getRemainingHits()).thenReturn(1000);

		SampleResoponceList testMessages = new SampleResoponceList();
		testMessages.add(firstMessage);
		testMessages.add(secondMessage);
		when(tw.getDirectMessages()).thenReturn(testMessages);
		
		testMessages = new SampleResoponceList();
		testMessages.add(thirdMessage);
		testMessages.add(fourthMessage);
		when(tw.getDirectMessages(Mockito.any(Paging.class))).thenReturn(testMessages);
	}

	@SuppressWarnings({ "rawtypes", "serial" })
	public static class SampleResoponceList extends ArrayList implements ResponseList {

		public RateLimitStatus getRateLimitStatus() {
			return mock(RateLimitStatus.class);
		}

		public RateLimitStatus getFeatureSpecificRateLimitStatus() {
			return mock(RateLimitStatus.class);
		}
		
	}
}
