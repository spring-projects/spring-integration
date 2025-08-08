/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.leader.event;

import org.springframework.integration.leader.Context;

/**
 * Generic event representing that leader has been revoked.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
@SuppressWarnings("serial")
public class OnRevokedEvent extends AbstractLeaderEvent {

	/**
	 * Instantiates a new revoked event.
	 *
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 * @param role the role of the leader
	 */
	public OnRevokedEvent(Object source, Context context, String role) {
		super(source, context, role);
	}

}
