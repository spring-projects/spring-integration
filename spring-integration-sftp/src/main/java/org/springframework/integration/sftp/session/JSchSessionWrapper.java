/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.sftp.session;

import java.util.concurrent.atomic.AtomicInteger;

import com.jcraft.jsch.Session;

/**
 * A wrapper for a JSch session that maintains a channel count and
 * physically disconnects when the last channel is closed.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
class JSchSessionWrapper {

	private final Session session;

	private final AtomicInteger channels = new AtomicInteger();

	JSchSessionWrapper(Session session) {
		this.session = session;
	}

	public void addChannel() {
		this.channels.incrementAndGet();
	}

	public void close() {
		if (channels.decrementAndGet() <= 0) {
			this.session.disconnect();
		}
	}

	public final Session getSession() {
		return session;
	}

	public boolean isConnected() {
		return session.isConnected();
	}

}
