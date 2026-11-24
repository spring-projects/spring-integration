/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.gateway;

import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 */
public class TestHandler {

	public String handle(Message<?> message) {
		return message.getPayload() + "!!!";
	}

}
