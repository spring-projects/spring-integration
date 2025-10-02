/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.stream;

import java.io.OutputStream;

/**
 * A {@link org.springframework.messaging.MessageHandler} that writes a byte array to an
 * {@link OutputStream}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.outbound.ByteStreamWritingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ByteStreamWritingMessageHandler extends
		org.springframework.integration.stream.outbound.ByteStreamWritingMessageHandler {

	public ByteStreamWritingMessageHandler(OutputStream stream) {
		this(stream, -1);
	}

	public ByteStreamWritingMessageHandler(OutputStream stream, int bufferSize) {
		super(stream, bufferSize);
	}

}
