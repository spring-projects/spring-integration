/*
 * Copyright 2019-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
