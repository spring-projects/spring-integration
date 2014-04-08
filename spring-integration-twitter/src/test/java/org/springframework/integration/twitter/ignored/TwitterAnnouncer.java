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

	public void dm(DirectMessage directMessage) {
		System.out.println("A direct message has been received from " +
				directMessage.getSender().getScreenName() + " with text " + directMessage.getText());
	}

	public void search(Message<?> search) {
		MessageHistory history = MessageHistory.read(search);
		System.out.println(history);
		Tweet tweet = (Tweet) search.getPayload();
		System.out.println("A search item was received " +
				tweet.getCreatedAt() + " with text " + tweet.getText());
	}

	public void mention(Tweet s) {
		System.out.println("A tweet mentioning (or replying) to you was received having text "
				+ s.getFromUser() + "-" +  s.getText() + " from " + s.getSource());
	}

	public void searchResult(Collection<Tweet> tweets) {
		if (tweets.size() == 0) {
			System.out.println("No results");
		}
		for (Tweet s : tweets) {
			System.out.println("Search result: "
					+ s.getFromUser() + "-" +  s.getText() + " from " + s.getSource());
		}
	}

	public void updates(Tweet t) {
		System.out.println("Received timeline update: " + t.getText() + " from " + t.getSource());
	}

}
