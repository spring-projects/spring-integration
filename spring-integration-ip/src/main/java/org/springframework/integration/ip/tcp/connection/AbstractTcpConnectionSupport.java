/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

/**
 * Base class for TCP Connection Support implementations.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public abstract class AbstractTcpConnectionSupport {

	private boolean pushbackCapable;

	private int pushbackBufferSize = 1;

	public boolean isPushbackCapable() {
		return this.pushbackCapable;
	}

	/**
	 * Set to true to cause wrapping of the connection's input stream in a
	 * {@link java.io.PushbackInputStream}, enabling deserializers to "unread" data.
	 * @param pushbackCapable true to enable.
	 */
	public void setPushbackCapable(boolean pushbackCapable) {
		this.pushbackCapable = pushbackCapable;
	}

	public int getPushbackBufferSize() {
		return this.pushbackBufferSize;
	}

	/**
	 * The size of the push back buffer; defaults to 1.
	 * @param pushbackBufferSize the size.
	 */
	public void setPushbackBufferSize(int pushbackBufferSize) {
		this.pushbackBufferSize = pushbackBufferSize;
	}

}
