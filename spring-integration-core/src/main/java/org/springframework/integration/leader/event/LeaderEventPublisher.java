/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.leader.event;

import org.springframework.integration.leader.Context;

/**
 * Interface for publishing leader based application events.
 *
 * @author Janne Valkealahti
 * @author Gary Russell
 * @author Glenn Renfro
 *
 */
public interface LeaderEventPublisher {

	/**
	 * Publish a granted event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 */
	void publishOnGranted(Object source, Context context, String role);

	/**
	 * Publish a revoked event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 */
	void publishOnRevoked(Object source, Context context, String role);

	/**
	 * Publish a failure to acquire event.
	 * @param source the component generated this event
	 * @param context the context associated with event
	 * @param role the role of the leader
	 * @since 5.0
	 */
	void publishOnFailedToAcquire(Object source, Context context, String role);

}
