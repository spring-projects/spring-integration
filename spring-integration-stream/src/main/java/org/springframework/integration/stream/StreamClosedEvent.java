/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.stream;

/**
 * Application event published when EOF is detected on a stream.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.event.StreamClosedEvent}
 */
@SuppressWarnings("serial")
@Deprecated(forRemoval = true, since = "7.0")
public class StreamClosedEvent extends org.springframework.integration.stream.event.StreamClosedEvent {

	public StreamClosedEvent(Object source) {
		super(source);
	}

}
