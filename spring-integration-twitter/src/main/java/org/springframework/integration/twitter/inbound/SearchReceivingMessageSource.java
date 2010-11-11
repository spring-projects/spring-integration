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

import org.springframework.integration.MessagingException;
import org.springframework.integration.twitter.core.SearchResults;
import org.springframework.integration.twitter.core.Tweet;
import org.springframework.integration.twitter.core.TwitterOperations;
import org.springframework.util.Assert;

/**
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SearchReceivingMessageSource extends AbstractTwitterMessageSource<Tweet> {
	/* since Twitter return 15 entries per page we need to be able to manage
	 * how many pages deep are we willing to go. Not sure yet about exposing this attribute via namespace
	 * but setting default to 10.
	 */
	private volatile int pageDepth = 10; 
	
	private volatile int currentPage = 1;
	private volatile String query;
	
	public SearchReceivingMessageSource(TwitterOperations twitter){
		super(twitter);
	}
	
	public void setQuery(String query) {
		Assert.hasText(query, "'query' must no be null");
		this.query = query;
	}
	
	@Override
	 public String getComponentType() {
		return "twitter:search-inbound-channel-adapter";  
	}

	@Override
	Runnable getApiCallback() {
		Runnable apiCallback = new Runnable() {	
			public void run() {
				try {
					if (tweets.size() <= prefetchThreshold){
						if (currentPage == pageDepth){
							currentPage = 1;
						}
						SearchResults results = twitter.search(query, currentPage, 0);
						List<Tweet> twetList = results.getTweets();
						if (currentPage == 1){
							forwardAll(twetList);
						}
						else {
							for (Tweet tweet : twetList) {
								tweets.add(tweet);
							}
						}
						if (twetList != null && twetList.size() > 0){
							currentPage++;
						}
						else {
							currentPage = 1;
						}
					}	
				} catch (Exception e) {
					if (e instanceof RuntimeException){
						throw (RuntimeException)e;
					}
					else {
						throw new MessagingException("Failed to poll for Twitter mentions updates", e);
					}
				}
			}
		};
		return apiCallback;
	}
}
