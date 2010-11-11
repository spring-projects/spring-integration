/*
 * Copyright 2002-2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.twitter.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.integration.Message;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.Twitter4jTemplate;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.integration.twitter.core.TwitterOperations;

import twitter4j.GeoLocation;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 *
 */
public class TimelineUpdateSendingMessageHandlerTests {
	
	TwitterOperations twitterOperations;
	Twitter twitter;
	
	@Before
	public void prepare() throws Exception{
		twitterOperations = spy(new Twitter4jTemplate());
		Field twitterField = Twitter4jTemplate.class.getDeclaredField("twitter");
		twitterField.setAccessible(true);
		twitter = mock(Twitter.class);
		twitterField.set(twitterOperations, twitter);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSendingStatusUpdate() throws Exception{
		TimelineSendingMessageHandler handler = new TimelineSendingMessageHandler(twitterOperations);
		Tweet tweet = new Tweet();
		tweet.setText("writing twitter tests");
		handler.handleMessage(new GenericMessage(tweet));
		verify(twitterOperations, times(1)).updateStatus(Mockito.any(String.class));
		verify(twitter, times(1)).updateStatus(Mockito.any(StatusUpdate.class));
	}
	@Test
	public void testSendingStatusUpdateWithHeaders() throws Exception{
		TimelineSendingMessageHandler handler = new TimelineSendingMessageHandler(twitterOperations);
		Message<?> message = MessageBuilder.withPayload("writing twitter tests")
		.setHeader(TwitterHeaders.IN_REPLY_TO_STATUS_ID, new Long(123))
		.setHeader(TwitterHeaders.PLACE_ID, "123")
		.setHeader(TwitterHeaders.GEOLOCATION, mock(GeoLocation.class))
		.setHeader(TwitterHeaders.DISPLAY_COORDINATES, true)
		.build();
		handler.handleMessage(message);
		verify(twitterOperations, times(1)).updateStatus(Mockito.any(String.class));
		verify(twitter, times(1)).updateStatus(Mockito.any(StatusUpdate.class));
	}
}
