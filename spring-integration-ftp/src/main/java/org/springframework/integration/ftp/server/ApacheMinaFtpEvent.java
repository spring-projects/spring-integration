/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpSession;

import org.springframework.integration.file.remote.server.FileServerEvent;

/**
 * {@code ApplicationEvent} generated from Apache Mina ftp events.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public abstract class ApacheMinaFtpEvent extends FileServerEvent {

	private static final long serialVersionUID = 1L;

	public ApacheMinaFtpEvent(Object source) {
		super(source);
	}

	public ApacheMinaFtpEvent(Object source, Throwable cause) {
		super(source, cause);
	}

	public FtpSession getSession() {
		return (FtpSession) source;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [clientAddress=" + getSession().getClientAddress() + "]";
	}

}
