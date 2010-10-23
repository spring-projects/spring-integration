/*
 * Copyright 2010 the original author or authors
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

package org.springframework.integration.twitter;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.twitter.model.Twitter4jGeoLocationImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Josh Long
 */
@ContextConfiguration
public class TestSendingUpdatesUsingNamespace extends AbstractJUnit4SpringContextTests {

	private MessagingTemplate messagingTemplate = new MessagingTemplate();

	@Value("#{out}") private MessageChannel channel;

	@Test
	@Ignore
	public void testSendingATweet() throws Throwable {
		MessageBuilder<String> mb = MessageBuilder.withPayload("simple test demonstrating the ability to encode location information")
				.setHeader(TwitterHeaders.TWITTER_IN_REPLY_TO_STATUS_ID, 21927437001L)
				.setHeader(TwitterHeaders.TWITTER_GEOLOCATION, new Twitter4jGeoLocationImpl(-76.226823, 23.642465)) // antarctica
				.setHeader(TwitterHeaders.TWITTER_DISPLAY_COORDINATES, true);
		Message<String> m = mb.build();
		this.messagingTemplate.send(this.channel, m);
	}
}
