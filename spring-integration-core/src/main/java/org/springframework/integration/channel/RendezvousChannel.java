/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
