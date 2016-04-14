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

package org.springframework.integration.twitter.ignored;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.history.MessageHistory;
import org.springframework.messaging.Message;
import org.springframework.social.twitter.api.DirectMessage;
import org.springframework.social.twitter.api.Tweet;
import org.springframework.stereotype.Component;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 *
 */
@Component
public class TwitterAnnouncer {

	private final Log logger = LogFactory.getLog(getClass());

	public void dm(DirectMessage directMessage) {
		logger.info("A direct message has been received from " +
				directMessage.getSender().getScreenName() + " with text " + directMessage.getText());
	}

	public void search(Message<?> search) {
		MessageHistory history = MessageHistory.read(search);
		Tweet tweet = (Tweet) search.getPayload();
		logger.info("A search item was received " +
				tweet.getCreatedAt() + " with text " + tweet.getText());
	}

	public void mention(Tweet s) {
		logger.info("A tweet mentioning (or replying) to you was received having text "
				+ s.getFromUser() + "-" +  s.getText() + " from " + s.getSource());
	}

	public void searchResult(Collection<Tweet> tweets) {
		if (tweets.size() == 0) {
			logger.info("No results");
		}
		for (Tweet s : tweets) {
			logger.info("Search result: "
					+ s.getFromUser() + "-" +  s.getText() + " from " + s.getSource());
		}
	}

	public void updates(Tweet t) {
		logger.info("Received timeline update: " + t.getText() + " from " + t.getSource());
	}

}
