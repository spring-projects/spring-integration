/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.List;

import org.springframework.social.twitter.api.DirectMessage;
import org.springframework.social.twitter.api.Twitter;

/**
 * This class handles support for receiving DMs (direct messages) using Twitter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class DirectMessageReceivingMessageSource extends AbstractTwitterMessageSource<DirectMessage> {

	public DirectMessageReceivingMessageSource(Twitter twitter, String metadataKey) {
		super(twitter, metadataKey);
	}

	@Override
	public String getComponentType() {
		return "twitter:dm-inbound-channel-adapter";
	}

	@Override
	protected List<DirectMessage> pollForTweets(long sinceId) {
		return this.getTwitter().directMessageOperations().getDirectMessagesReceived(1, this.getPageSize(), sinceId, 0);
	}

}
