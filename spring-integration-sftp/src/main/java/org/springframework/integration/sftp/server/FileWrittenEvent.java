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

package org.springframework.integration.sftp.server;

import java.nio.file.Path;

import org.springframework.lang.Nullable;

/**
 * An event that is emitted when a file is written.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class FileWrittenEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private final String remoteHandle;

	private transient Path file;

	private final int dataLen;

	public FileWrittenEvent(Object source, String remoteHandle, Path file, int dataLen, @Nullable Throwable thrown) {
		super(source, thrown);
		this.remoteHandle = remoteHandle;
		this.file = file;
		this.dataLen = dataLen;
	}

	public String getRemoteHandle() {
		return this.remoteHandle;
	}

	@Nullable
	public Path getFile() {
		return this.file;
	}

	public int getDataLen() {
		return this.dataLen;
	}

	@Override
	public String toString() {
		return "FileWrittenEvent [remoteHandle=" + this.remoteHandle
				+ ", file=" + this.file
				+ ", dataLen=" + this.dataLen
				+ (this.cause == null ? "" : ", cause=" + this.cause)
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
