/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
