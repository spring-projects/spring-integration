/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
