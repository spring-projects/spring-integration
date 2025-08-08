/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import java.util.concurrent.SynchronousQueue;

import org.springframework.messaging.Message;

/**
 * A zero-capacity version of {@link QueueChannel} that delegates to a
 * {@link SynchronousQueue} internally. This accommodates "handoff" scenarios
 * (i.e. blocking while waiting for another party to send or receive).
 *
 * @author Mark Fisher
 */
public class RendezvousChannel extends QueueChannel {

	public RendezvousChannel() {
		super(new SynchronousQueue<Message<?>>());
	}

}
