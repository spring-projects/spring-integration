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
	
	List<DirectMessage> getDirectMessages();
	
	List<DirectMessage> getDirectMessages(Paging paging);
	
	List<Status> getMentions();
	
	List<Status> getMentions(Paging paging);
	
	List<Status> getFriendsTimeline();
	
	List<Status> getFriendsTimeline(Paging paging);
	
	void sendDirectMessage(String userName, String text);
	
	void sendDirectMessage(int userId, String text);
	
	void updateStatus(StatusUpdate status);
}
