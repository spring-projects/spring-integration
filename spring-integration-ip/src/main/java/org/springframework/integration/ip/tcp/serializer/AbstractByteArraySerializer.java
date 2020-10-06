/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import java.io.IOException;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;

/**
 * Base class for (de)serializers that provide a mechanism to
 * reconstruct a byte array from an arbitrary stream.
 *
 * @author Gary Russell
 * @author Artme Bilan
 *
 * @since 2.0
 *
 */
public abstract class AbstractByteArraySerializer implements
		Serializer<byte[]>,
		Deserializer<byte[]>,
		ApplicationEventPublisherAware {

	/**
	 * The default maximum message size when deserializing.
	 * @since 5.1.3
	 */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 2048;

	protected final LogAccessor logger = new LogAccessor(this.getClass()); // NOSONAR

	private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * The maximum supported message size for this serializer.
	 * Default 2048.
	 * @return The max message size.
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}

	/**
	 * The maximum supported message size for this serializer.
	 * Default 2048.
	 * @param maxMessageSize The max message size.
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	protected void checkClosure(int bite) throws IOException {
		if (bite < 0) {
			this.logger.debug("Socket closed during message assembly");
			throw new IOException("Socket closed during message assembly");
		}
	}

	protected void publishEvent(Exception cause, byte[] buffer, int offset) {
		TcpDeserializationExceptionEvent event = new TcpDeserializationExceptionEvent(this, cause, buffer, offset);
		if (this.applicationEventPublisher != null) {
			this.applicationEventPublisher.publishEvent(event);
		}
		else {
			this.logger.trace(() -> "No event publisher for " + event);
		}
	}

}
