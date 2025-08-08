/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support;

/**
 * Indicates the mode of operation for the outbound Jpa Gateway.
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public enum OutboundGatewayType {
	UPDATING, RETRIEVING
}
