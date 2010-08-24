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

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.util.Assert;
import twitter4j.TwitterException;


/**
 * Simple adapter to support sending outbound direct messages ("DM"s) using twitter
 *
 * @author Josh Long
 * @see org.springframework.integration.twitter.TwitterHeaders
 * @see twitter4j.Twitter
 */
public class OutboundDMStatusMessageHandler extends AbstractOutboundTwitterEndpointSupport {
	public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException, MessageDeliveryException {
		try {
			String txt = (String) message.getPayload();
			Object toUser =
					message.getHeaders().containsKey(TwitterHeaders.TWITTER_DM_TARGET_USER_ID) ?
							message.getHeaders().get(TwitterHeaders.TWITTER_DM_TARGET_USER_ID) :
							null;

			Assert.notNull(txt, "the message payload must be a String to be used as the direct message body text");

			Assert.notNull(toUser, "the header '" + TwitterHeaders.TWITTER_DM_TARGET_USER_ID + "' must be present");

			Assert.state(toUser instanceof String || toUser instanceof Integer,
					"the header '" + TwitterHeaders.TWITTER_DM_TARGET_USER_ID + "' must be either a String (a screenname) or an int (a user ID)");

			if (toUser instanceof Integer) {
				this.twitter.sendDirectMessage((Integer) toUser, txt);
			} else if (toUser instanceof String) {
				this.twitter.sendDirectMessage((String) toUser, txt);
			}
		} catch (TwitterException e) {
			logger.debug(e);
			throw new RuntimeException(e);
		}
	}
}
