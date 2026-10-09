/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import org.apache.sshd.server.session.ServerSession;
import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.server.FileServerEvent;

/**
 * {@code ApplicationEvent} generated from Apache Mina sftp events.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public abstract class ApacheMinaSftpEvent extends FileServerEvent {

	private static final long serialVersionUID = 1L;

	public ApacheMinaSftpEvent(Object source) {
		super(source);
	}

	public ApacheMinaSftpEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

	public ServerSession getSession() {
		return (ServerSession) source;
	}

}
