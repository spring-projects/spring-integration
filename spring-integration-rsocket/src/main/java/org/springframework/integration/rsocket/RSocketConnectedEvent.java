/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket;

import java.util.Map;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.integration.events.IntegrationEvent;
import org.springframework.messaging.rsocket.RSocketRequester;

/**
 * An {@link IntegrationEvent} to indicate that {@code RSocket} from the client is connected
 * to the server.
 * <p>
 * This event can be used for mapping {@link RSocketRequester} to the client by the
 * {@code headers} meta-data or connect payload {@code data}.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 *
 * @see IntegrationRSocketMessageHandler
 */
@SuppressWarnings("serial")
public class RSocketConnectedEvent extends IntegrationEvent {

	private final Map<String, Object> headers;

	private final DataBuffer data;

	private final RSocketRequester requester;

	public RSocketConnectedEvent(Object source, Map<String, Object> headers, DataBuffer data,
			RSocketRequester requester) {

		super(source);
		this.headers = headers;
		this.data = data;
		this.requester = requester;
	}

	public Map<String, Object> getHeaders() {
		return this.headers;
	}

	public DataBuffer getData() {
		return this.data;
	}

	public RSocketRequester getRequester() {
		return this.requester;
	}

	@Override
	public String toString() {
		return "RSocketConnectedEvent{" +
				"headers=" + this.headers +
				", data=" + this.data +
				", requester=" + this.requester +
				'}';
	}

}
