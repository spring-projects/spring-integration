/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class FileIntegrationEvent extends IntegrationEvent {

	public FileIntegrationEvent(Object source) {
		super(source);
	}

	private FileIntegrationEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
