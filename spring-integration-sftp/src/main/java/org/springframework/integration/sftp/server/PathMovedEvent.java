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
 * An event emitted when a path is moved.
 * @author Gary Russell
 *
 * @since 5.2
 *
 */
public class PathMovedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private transient Path srcPath;

	private transient Path dstPath;

	public PathMovedEvent(Object source, Path srcPath, Path dstPath, @Nullable Throwable thrown) {
		super(source, thrown);
		this.srcPath = srcPath;
		this.dstPath = dstPath;
	}

	@Nullable
	public Path getSrcPath() {
		return this.srcPath;
	}

	@Nullable
	public Path getDstPath() {
		return this.dstPath;
	}

	@Override
	public String toString() {
		return "PathMovedEvent [srcPath=" + this.srcPath
				+ ", dstPath=" + this.dstPath
				+ (this.cause == null ? "" : ", cause=" + this.cause)
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
