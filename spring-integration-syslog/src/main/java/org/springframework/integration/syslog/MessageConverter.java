/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.syslog;

import org.springframework.messaging.Message;

/**
 * A converter to convert the raw message created by the underlying
 * UDP/TCP endpoint to a specific form of Syslog message.
 * @author Gary Russell
 * @since 3.0
 *
 */
@FunctionalInterface
public interface MessageConverter {

	Message<?> fromSyslog(Message<?> syslog);

}
