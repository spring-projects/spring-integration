/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.acks;

/**
 * Opaque object for manually acknowledging.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
@FunctionalInterface
public interface SimpleAcknowledgment {

	/**
	 * Acknowledge the message delivery.
	 */
	void acknowledge();

}
