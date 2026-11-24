/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.websocket.event;

import org.springframework.messaging.Message;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;

/**
 * The {@link AbstractSubProtocolEvent} implementation, which is emitted
 * for the WebSocket sub-protocol-specific {@code RECEIPT} frame on the client side.
 *
 * @author Artem Bilan
 * @since 4.1.3
 */
@SuppressWarnings("serial")
public class ReceiptEvent extends AbstractSubProtocolEvent {

	public ReceiptEvent(Object source, Message<byte[]> message) {
		super(source, message);
	}

}
