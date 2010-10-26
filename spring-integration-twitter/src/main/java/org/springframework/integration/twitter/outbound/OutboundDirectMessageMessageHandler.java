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
package org.springframework.integration.twitter.outbound;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.twitter.core.TwitterHeaders;
import org.springframework.util.Assert;

import twitter4j.TwitterException;


/**
 * Simple adapter to support sending outbound direct messages ("DM"s) using twitter
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class OutboundDirectMessageMessageHandler extends AbstractOutboundTwitterEndpointSupport {
	
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		if (this.twitter == null){
			this.afterPropertiesSet();
		}
		try {
			Object payload = (String) message.getPayload();
			Assert.isInstanceOf(String.class, payload, "Only payload of type String is supported. If your payload " +
					"is not of type String you may want to introduce transformer");
			Assert.isTrue(message.getHeaders().containsKey(TwitterHeaders.TWITTER_DM_TARGET_USER_ID), 
					"You must provide '" + TwitterHeaders.TWITTER_DM_TARGET_USER_ID + "' header");
			Object toUser = message.getHeaders().get(TwitterHeaders.TWITTER_DM_TARGET_USER_ID);

			Assert.state(toUser instanceof String || toUser instanceof Integer,
					"the header '" + TwitterHeaders.TWITTER_DM_TARGET_USER_ID + 
					"' must be either a String (a screenname) or an int (a user ID)");

			if (toUser instanceof Integer) {
				this.twitter.sendDirectMessage((Integer) toUser, (String) payload);
			} 
			else if (toUser instanceof String) {
				this.twitter.sendDirectMessage((String) toUser, (String) payload);
			}
		} catch (TwitterException e) {
			throw new MessageHandlingException(message, e);
		}
	}
}
