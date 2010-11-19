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

package org.springframework.integration.twitter.core;

import java.util.List;

import twitter4j.Twitter;

/**
 * @author Craig Walls
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public interface TwitterOperations {

	/**
	 * Retrieves the user's Twitter screen name.
	 * 
	 * @return the user's screen name at Twitter
	 */
	String getProfileId();
	
	/**
	 * Updates the user's status.
	 * 
	 * @param status
	 *            The status message
	 * 
	 */
	void updateStatus(String status);
	
	/**
	 * Searches Twitter, returning the first page of {@link Tweet}s
	 * 
	 * @param query
	 *            The search query string
	 * @return a {@link SearchResults} containing {@link Tweet}s
	 * 
	 */
	SearchResults search(String query);

	/**
	 * Searches Twitter, returning a specific page out of the complete set of
	 * results.
	 * 
	 * @param query
	 *            The search query string
	 * @param sinceId
	 *            The minimum {@link Tweet} ID to return in the results
	 * 
	 * @return a {@link SearchResults} containing {@link Tweet}s
	 * 
	 */
	SearchResults search(String query, long sinceId);

	List<Tweet> getDirectMessages();
	
	List<Tweet> getDirectMessages(long sinceId);
	
	List<Tweet> getMentions();
	
	List<Tweet> getMentions(long sinceId);
	
	List<Tweet> getTimeline();
	
	List<Tweet> getTimeline(long sinceId);
	
	void sendDirectMessage(String userName, String text);
	
	void sendDirectMessage(int userId, String text);

	/**
	 * Temporary method. Should be removed one migrated to Spring Social
	 */
	Twitter getUnderlyingTwitter();

}
