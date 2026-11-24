/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.store;

/**
 * A {@link ChannelMessageStore} that supports the
 * notion of message priority. It is left to implementations to determine what
 * that means and whether all or a subset of priorities are supported.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface PriorityCapableChannelMessageStore extends ChannelMessageStore {

	/**
	 * @return true if message priority is enabled in this channel message store.
	 */
	boolean isPriorityEnabled();

}
