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

import java.util.List;

import org.springframework.social.twitter.api.Tweet;
import org.springframework.social.twitter.api.Twitter;

/**
 * Handles forwarding all new {@link twitter4j.Status} that are 'replies' or 'mentions' to some other tweet.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class MentionsReceivingMessageSource extends AbstractTwitterMessageSource<Tweet> {

	public MentionsReceivingMessageSource(Twitter twitter){
		super(twitter);
	}


	@Override
	public String getComponentType() {
		return "twitter:mention-inbound-channel-adapter";
	}

	@Override
	protected List<?> pollForTweets(long sinceId) {
		return (sinceId > 0) ? this.getTwitter().timelineOperations().getMentions(1, 20, sinceId, 0) : this.getTwitter().timelineOperations().getMentions(1, 50, 0, 0);
	}

}
