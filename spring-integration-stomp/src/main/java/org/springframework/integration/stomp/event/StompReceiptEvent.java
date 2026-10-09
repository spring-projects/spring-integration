/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.event;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;

/**
 * The {@link StompIntegrationEvent} for the STOMP {@code RECEIPT} Frames or lost receipts.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.2
 * @see org.springframework.integration.stomp.inbound.StompInboundChannelAdapter
 * @see org.springframework.integration.stomp.outbound.StompMessageHandler
 */
@SuppressWarnings("serial")
public class StompReceiptEvent extends StompIntegrationEvent {

	private final @Nullable String destination;

	private final @Nullable String receiptId;

	private final StompCommand stompCommand;

	private final boolean lost;

	private @Nullable Message<?> message;

	public StompReceiptEvent(Object source, @Nullable String destination, @Nullable String receiptId,
		StompCommand stompCommand, boolean lost) {

		super(source);
		this.destination = destination;
		this.receiptId = receiptId;
		this.stompCommand = stompCommand;
		this.lost = lost;
	}

	public @Nullable String getDestination() {
		return this.destination;
	}

	public @Nullable String getReceiptId() {
		return this.receiptId;
	}

	public StompCommand getStompCommand() {
		return this.stompCommand;
	}

	public boolean isLost() {
		return this.lost;
	}

	public @Nullable Message<?> getMessage() {
		return this.message;
	}

	public void setMessage(Message<?> message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "StompReceiptEvent [destination=" + this.destination + ", receiptId=" + this.receiptId + ", stompCommand="
				+ this.stompCommand + ", lost=" + this.lost + ", message=" + this.message + "]";
	}

}
