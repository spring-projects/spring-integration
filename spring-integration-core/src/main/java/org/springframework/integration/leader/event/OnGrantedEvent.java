/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.leader.event;

import org.springframework.integration.leader.Context;

/**
 * Generic event representing that leader has been granted.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 *
 */
@SuppressWarnings("serial")
public class OnGrantedEvent extends AbstractLeaderEvent {

	/**
	 * Instantiates a new granted event.
	 *
	 * @param source the component that published the event (never {@code null})
	 * @param context the context associated with this event
	 * @param role the role of the leader
	 */
	public OnGrantedEvent(Object source, Context context, String role) {
		super(source, context, role);
	}

}
