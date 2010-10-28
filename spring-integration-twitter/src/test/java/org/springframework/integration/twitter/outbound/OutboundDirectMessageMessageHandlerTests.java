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

package org.springframework.integration.twitter.outbound;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.integration.twitter.oauth.OAuthConfiguration;

import twitter4j.GeoLocation;
import twitter4j.Twitter;

/**
 * @author Oleg Zhurakousky
 */
public class OutboundDirectMessageMessageHandlerTests {

	private Twitter twitter;

	@Test
	public void validateSendDirectMessage() throws Exception{
		MessageBuilder<String> mb = MessageBuilder.withPayload("hello")
			.setHeader(TwitterHeaders.GEOLOCATION, new GeoLocation(-76.226823, 23.642465)) // antarctica
			.setHeader(TwitterHeaders.DISPLAY_COORDINATES, true)
			.setHeader(TwitterHeaders.DM_TARGET_USER_ID, "foo");

		OutboundDirectMessageMessageHandler handler = new OutboundDirectMessageMessageHandler();
		handler.setConfiguration(this.getTestConfiguration());
		handler.afterPropertiesSet();
		
		handler.handleMessage(mb.build());
		verify(twitter, times(1)).sendDirectMessage("foo", "hello");
		
		mb = MessageBuilder.withPayload("hello")
			.setHeader(TwitterHeaders.GEOLOCATION, new GeoLocation(-76.226823, 23.642465)) // antarctica
			.setHeader(TwitterHeaders.DISPLAY_COORDINATES, true)
			.setHeader(TwitterHeaders.DM_TARGET_USER_ID, 123);

		handler.handleMessage(mb.build());
		verify(twitter, times(1)).sendDirectMessage(123, "hello");
	}


	private OAuthConfiguration getTestConfiguration() throws Exception {
		twitter = mock(Twitter.class);
		OAuthConfiguration configuration = mock(OAuthConfiguration.class);
		when(configuration.getTwitter()).thenReturn(twitter);
		return configuration;
	}

}
