/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
