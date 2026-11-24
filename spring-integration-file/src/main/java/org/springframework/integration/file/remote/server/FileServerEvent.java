/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.remote.server;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.events.IntegrationEvent;

/**
 * Base class for file server events. Typically, the source for these events will be some
 * kind of client/server session object containing information such as the client's ip
 * address.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
@SuppressWarnings("serial")
public abstract class FileServerEvent extends IntegrationEvent {

	public FileServerEvent(Object source) {
		super(source);
	}

	public FileServerEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
