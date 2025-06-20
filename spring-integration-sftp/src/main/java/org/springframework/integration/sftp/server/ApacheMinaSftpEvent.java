/*
 * Copyright 2019-present the original author or authors.
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

package org.springframework.integration.sftp.server;

import org.apache.sshd.server.session.ServerSession;

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

	public ApacheMinaSftpEvent(Object source, Throwable cause) {
		super(source, cause);
	}

	public ServerSession getSession() {
		return (ServerSession) source;
	}

}
