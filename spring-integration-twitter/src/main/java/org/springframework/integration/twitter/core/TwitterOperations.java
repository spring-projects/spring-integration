/**
 * 
 */
package org.springframework.integration.twitter.core;

import java.util.List;

import twitter4j.DirectMessage;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.StatusUpdate;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 *
 */
public interface TwitterOperations {

	String getProfileId();
	
	RateLimitStatus getRateLimitStatus();
	
	List<Tweet> getDirectMessages();
	
	List<Tweet> getDirectMessages(Paging paging);
	
	List<Tweet> getMentions();
	
	List<Tweet> getMentions(Paging paging);
	
	List<Tweet> getFriendsTimeline();
	
	List<Tweet> getFriendsTimeline(Paging paging);
	
	void sendDirectMessage(String userName, String text);
	
	void sendDirectMessage(int userId, String text);
	
	void updateStatus(Tweet status);
}
