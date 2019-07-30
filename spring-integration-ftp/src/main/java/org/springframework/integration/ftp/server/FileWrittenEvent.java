/*
 * Copyright 2019 the original author or authors.
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
 * An event that is emitted when a file is written.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class FileWrittenEvent extends FtpRequestEvent {

	private static final long serialVersionUID = 1L;

	private final boolean append;

	public FileWrittenEvent(FtpSession source, FtpRequest request, boolean append) {
		super(source, request);
		this.append = append;
	}

	public boolean isAppend() {
		return this.append;
	}

	@Override
	public String toString() {
		return "FileWrittenEvent [append=" + this.append
				+ ", request=" + this.request
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
