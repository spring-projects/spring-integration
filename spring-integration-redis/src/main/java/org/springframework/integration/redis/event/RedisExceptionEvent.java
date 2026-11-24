/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.redis.event;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisExceptionEvent extends RedisIntegrationEvent {

	public RedisExceptionEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
