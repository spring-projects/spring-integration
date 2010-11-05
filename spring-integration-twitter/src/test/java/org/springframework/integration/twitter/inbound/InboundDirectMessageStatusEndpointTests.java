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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import twitter4j.DirectMessage;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 */
public class InboundDirectMessageStatusEndpointTests {

	private DirectMessage firstMessage;

	private DirectMessage secondMessage;

	private Twitter twitter = mock(Twitter.class);


	@Before
	public void prepare() {
		twitter = mock(Twitter.class);
		firstMessage = mock(DirectMessage.class);
		when(firstMessage.getCreatedAt()).thenReturn(new Date(5555555555L));
		when(firstMessage.getId()).thenReturn(200);
		secondMessage = mock(DirectMessage.class);
		when(secondMessage.getCreatedAt()).thenReturn(new Date(2222222222L));
		when(secondMessage.getId()).thenReturn(2000);
	}


	@Test
	public void testTwitterMockedUpdates() throws Exception{
//		QueueChannel channel = new QueueChannel();
//		DirectMessageReceivingMessageSource endpoint = new DirectMessageReceivingMessageSource(twitter);
//		//endpoint.setOutputChannel(channel);
//		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//		scheduler.afterPropertiesSet();
//		endpoint.setTaskScheduler(scheduler);
//		//endpoint.setConfiguration(this.getTestConfigurationForDirectMessages());
//		endpoint.setBeanName("twitterEndpoint");
//		endpoint.afterPropertiesSet();
//		endpoint.start();
//		Message<?> message1 = channel.receive(3000);
//		assertNotNull(message1);
//		// should be second message since its timestamp is newer
//		assertEquals(secondMessage.getId(), ((DirectMessage)message1.getPayload()).getId());
//		Message<?> message2 = channel.receive(100);
//		assertNull(message2); // should be null, since 
	}


//	@SuppressWarnings("unchecked")
//	private OAuthConfiguration getTestConfigurationForDirectMessages() throws Exception{
//		OAuthConfiguration configuration = mock(OAuthConfiguration.class);
//		RateLimitStatus rateLimitStatus = mock(RateLimitStatus.class);
//		when(twitter.getRateLimitStatus()).thenReturn(rateLimitStatus);
//		when(configuration.getTwitter()).thenReturn(twitter);
//		when(rateLimitStatus.getSecondsUntilReset()).thenReturn(2464);
//		when(rateLimitStatus.getRemainingHits()).thenReturn(250);
//
//		ResponseList<DirectMessage> responses = mock(ResponseList.class);
//		List<DirectMessage> testMessages = new ArrayList<DirectMessage>();
//		testMessages.add(firstMessage);
//		testMessages.add(secondMessage);
//
//		when(responses.iterator()).thenReturn(testMessages.iterator());
//		when(twitter.getDirectMessages()).thenReturn(responses);
//		when(twitter.getDirectMessages(Mockito.any(Paging.class))).thenReturn(responses);
//		return configuration;
//	}

}
