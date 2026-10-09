/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.ftp.server;

import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;

/**
 * Base class for all events having an {@link FtpRequest}.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public abstract class FtpRequestEvent extends ApacheMinaFtpEvent {

	private static final long serialVersionUID = 1L;

	protected final transient FtpRequest request; //NOSONAR protected final

	public FtpRequestEvent(FtpSession source, FtpRequest request) {
		super(source);
		this.request = request;
	}

	public FtpRequest getRequest() {
		return this.request;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [request=" + this.request
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
