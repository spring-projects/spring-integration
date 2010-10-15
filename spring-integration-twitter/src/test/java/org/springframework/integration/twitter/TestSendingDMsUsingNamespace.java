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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.util.StringUtils;

import twitter4j.GeoLocation;

/**
 * @author Josh Long
 */
@ContextConfiguration
public class TestSendingDMsUsingNamespace extends AbstractJUnit4SpringContextTests {
	private volatile MessagingTemplate messagingTemplate = new MessagingTemplate();
	@Value("#{out}")
	private MessageChannel channel;

	@Test
	@Ignore
	public void testSendingATweet() throws Throwable {
		String dmUsr = System.getProperties().getProperty("twitter.dm.user");
		MessageBuilder<String> mb = MessageBuilder.withPayload("'Hello world!', from the Spring Integration outbound Twitter adapter")
				.setHeader(TwitterHeaders.TWITTER_GEOLOCATION, new GeoLocation(-76.226823, 23.642465)) // antarctica
				.setHeader(TwitterHeaders.TWITTER_DISPLAY_COORDINATES, true);

		if (StringUtils.hasText(dmUsr)) {
			mb.setHeader(TwitterHeaders.TWITTER_DM_TARGET_USER_ID, dmUsr);
		}

		Message<String> m = mb.build();

		this.messagingTemplate.send(this.channel, m);
	}
}
