/*
 * Copyright 2014-2024 the original author or authors.
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

import java.io.Serial;

import org.springframework.integration.ip.event.IpIntegrationEvent;

/**
 * Event representing an exception while decoding an incoming stream.
 * Contains the buffer of data decoded so far and the offset in the
 * buffer where the exception occurred, if available, otherwise -1.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class TcpDeserializationExceptionEvent extends IpIntegrationEvent {

	@Serial
	private static final long serialVersionUID = 8812537718016054732L;

	private final byte[] buffer;

	private final int offset;

	public TcpDeserializationExceptionEvent(Object source, Throwable cause, byte[] buffer, //NOSONAR - direct storage
			int offset) {
		super(source, cause);
		this.buffer = buffer; //NOSONAR - direct storage
		this.offset = offset;
	}

	public byte[] getBuffer() {
		return this.buffer; //NOSONAR - direct access
	}

	public int getOffset() {
		return this.offset;
	}

}
