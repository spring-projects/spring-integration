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
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * An event emitted when a directory is created.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class DirectoryCreatedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private transient Path path;

	private final Map<String, ?> attrs;

	public DirectoryCreatedEvent(Object source, Path path, Map<String, ?> attrs) {
		super(source);
		this.path = path;
		this.attrs = attrs;
	}

	@Nullable
	public Path getPath() {
		return this.path;
	}

	public Map<String, ?> getAttrs() {
		return this.attrs;
	}

	@Override
	public String toString() {
		return "DirectoryCreatedEvent [path=" + this.path
				+ ", attrs=" + this.attrs
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
