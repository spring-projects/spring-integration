package org.springframework.integration.twitter.model;

/**
 * describes a simple status message on twitter. this could be a message
 * that updates a timeline on behalf of a user, or it could be a message that contains a reply.
 *
 *
 * @author Josh Long
 */
public interface Status {

	java.util.Date getCreatedAt();

	long getId();

	java.lang.String getText();

	java.lang.String getSource();

	boolean isTruncated();

	long getInReplyToStatusId();

	int getInReplyToUserId();

	java.lang.String getInReplyToScreenName();

	boolean isFavorited();

	 User getUser();

	boolean isRetweet();

	 Status getRetweetedStatus();

	java.lang.String[] getContributors();

}
