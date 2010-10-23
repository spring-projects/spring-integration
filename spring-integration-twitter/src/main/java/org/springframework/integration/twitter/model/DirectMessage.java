package org.springframework.integration.twitter.model;

/**
 * Describes a direct-message in twitter. (Also known as a "DM").
 * <p/>
 * these are messages sent privately to a user.
 *
 * @author Josh Long
 */
public interface DirectMessage {
	int getId();

	java.lang.String getText();

	java.util.Date getCreatedAt();

	User getSender() ;

	User getRecipient();

 }
