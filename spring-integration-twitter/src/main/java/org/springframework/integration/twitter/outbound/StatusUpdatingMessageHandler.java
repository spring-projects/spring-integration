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

import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;

/**
 * MessageHandler for sending regular status updates as well as 'replies' or 'mentions'.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class StatusUpdatingMessageHandler extends AbstractMessageHandler {

	private final TwitterOperations twitterOperations;


	public StatusUpdatingMessageHandler(TwitterOperations twitterOperations) {
		Assert.notNull(twitterOperations, "twitterOperations must not be null");
		this.twitterOperations = twitterOperations;
	}


	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object payload = message.getPayload();
		String statusText = null;
		if (payload instanceof Tweet) {
			statusText = ((Tweet) payload).getText();
		}
		else if (payload instanceof String) {
			statusText = (String) payload;
		}
		else {
			throw new MessageHandlingException(message, "Unsupported payload type '" + payload.getClass().getName() + "'");
		}
		this.twitterOperations.updateStatus(statusText);
	}

}
