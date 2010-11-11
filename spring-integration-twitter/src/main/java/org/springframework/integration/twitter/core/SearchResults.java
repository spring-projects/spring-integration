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

package org.springframework.integration.twitter.core;

import java.util.List;

/**
 * Represents the results of a Twitter search, including matching {@link Tweet}s
 * and any metadata associated with that search.
 * 
 * @author Craig Walls
 */
public class SearchResults {

	private List<Tweet> tweets;

	private long maxId;

	private long sinceId;

	private boolean lastPage;


	public SearchResults(List<Tweet> tweets, long maxId, long sinceId, boolean lastPage) {
		this.tweets = tweets;
		this.maxId = maxId;
		this.sinceId = sinceId;
		this.lastPage = lastPage;
	}


	/**
	 * Returns the list of matching {@link Tweet}s
	 */
	public List<Tweet> getTweets() {
		return tweets;
	}

	/**
	 * Returns the maximum {@link Tweet} ID in the search results
	 */
	public long getMaxId() {
		return maxId;
	}

	/**
	 * Returns the {@link Tweet} ID after which all of the matching {@link Tweet}s were created
	 */
	public long getSinceId() {
		return sinceId;
	}

	/**
	 * Returns <code>true</code> if this is the last page of matching {@link Tweet}s,
	 * <code>false</code> if there are more pages that follow this one.
	 */
	public boolean isLastPage() {
		return lastPage;
	}

}
