/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.router.config;

import org.springframework.integration.splitter.AbstractMessageSplitter;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class TestSplitterImpl extends AbstractMessageSplitter {

	@Override
	protected Object splitMessage(Message<?> message) {
		return message.getPayload().toString().split("\\.");
	}

}
