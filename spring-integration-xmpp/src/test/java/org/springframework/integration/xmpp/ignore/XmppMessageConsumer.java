/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.xmpp.ignore;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.packet.Message;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.stereotype.Component;

/**
 * Handle display of incoming XMPP messages to this user.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@Component
public class XmppMessageConsumer {

	private final Log logger = LogFactory.getLog(getClass());

	@ServiceActivator
	public void consume(Object input) {
		String text;
		if (input instanceof Message) {
			text = ((Message) input).getBody();
		}
		else if (input instanceof String) {
			text = (String) input;
		}
		else {
			throw new IllegalArgumentException(
					"expected either a Smack Message or a String, but received: " + input);
		}
		logger.info("================================================================================");
		logger.info("message: " + text);
	}

}
