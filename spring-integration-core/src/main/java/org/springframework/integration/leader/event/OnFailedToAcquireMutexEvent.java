/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.leader.event;

import org.springframework.integration.leader.Context;

/**
 * Generic event representing that a mutex could not be acquired during leader election.
 *
 * @author Glenn Renfro
 * @author Artem Bilan
 *
 * @since 5.0
 */
@SuppressWarnings("serial")
public class OnFailedToAcquireMutexEvent extends AbstractLeaderEvent {

	/**
	 * Instantiate a new OnFailedToAcquireMutexEvent.
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 * @param role the role of the leader
	 */
	public OnFailedToAcquireMutexEvent(Object source, Context context, String role) {
		super(source, context, role);
	}

}
