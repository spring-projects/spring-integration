/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

/**
 * @author Mark Fisher
 */
public class ExceptionThrowingTestBean {

	public void handle(Message<?> message) {
		throw new MessageHandlingException(message, "intentional test failure");
	}

}
