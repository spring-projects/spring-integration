/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.redis.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class RedisIntegrationEvent extends IntegrationEvent {

	public RedisIntegrationEvent(Object source) {
		super(source);
	}

	public RedisIntegrationEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
