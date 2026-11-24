/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * A {@link MessageGroupProcessor} that simply returns the messages in the group.
 * It can be used to configure an aggregator as a barrier, such that when the group
 * is complete, the grouped messages are released as individual messages.
 *
 * @author Gary Russell
 * @since 4.2
 *
 */
public class SimpleMessageGroupProcessor implements MessageGroupProcessor {

	@Override
	public Object processMessageGroup(MessageGroup group) {
		return group.getMessages();
	}

}
