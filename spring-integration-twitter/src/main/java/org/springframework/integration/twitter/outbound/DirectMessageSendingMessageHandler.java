/*
 * Copyright 2002-2014 the original author or authors
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

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.messaging.Message;
import org.springframework.social.twitter.api.Twitter;
import org.springframework.util.Assert;

/**
 * Simple adapter to support sending outbound direct messages ("DM"s) using Twitter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 2.0
 */
public class DirectMessageSendingMessageHandler extends AbstractMessageHandler {

	private final Twitter twitter;


	public DirectMessageSendingMessageHandler(Twitter twitter) {
		Assert.notNull(twitter, "twitter must not be null");
		this.twitter = twitter;
	}

	@Override
	public String getComponentType() {
		return "twitter:dm-outbound-channel-adapter";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.isTrue(message.getPayload() instanceof String, "Only payload of type String is supported. " +
				"Consider adding a transformer to the message flow in front of this adapter.");
		Object toUser = message.getHeaders().get(TwitterHeaders.DM_TARGET_USER_ID);
		Assert.isTrue(toUser instanceof String || toUser instanceof Number,
				"the header '" + TwitterHeaders.DM_TARGET_USER_ID +
				"' must contain either a String (a screenname) or an number (a user ID)");
		String payload = (String) message.getPayload();
		if (toUser instanceof Number) {
			this.twitter.directMessageOperations().sendDirectMessage(((Number) toUser).longValue(), payload);
		}
		else if (toUser instanceof String) {
			this.twitter.directMessageOperations().sendDirectMessage((String) toUser, payload);
		}
	}

}
