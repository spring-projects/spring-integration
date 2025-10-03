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
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.jspecify.annotations.Nullable;

/**
 * A {@link org.springframework.messaging.MessageHandler}
 * that writes characters to a {@link Writer}.
 * String, character array, and byte array payloads will be written directly,
 * but for other payload types, the result of the object's {@link #toString()}
 * method will be written. To append a new-line after each write, set the
 * {@link #setShouldAppendNewLine(boolean) shouldAppendNewLine} flag to 'true'. It is 'false' by default.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 *  @deprecated since 7.0 in favor of {@link org.springframework.integration.stream.outbound.CharacterStreamWritingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class CharacterStreamWritingMessageHandler extends
		org.springframework.integration.stream.outbound.CharacterStreamWritingMessageHandler {

	public CharacterStreamWritingMessageHandler(Writer writer) {
		this(writer, -1);
	}

	public CharacterStreamWritingMessageHandler(Writer writer, int bufferSize) {
		super(writer, bufferSize);
	}

	/**
	 * Factory method that creates a target for stdout (System.out) with the
	 * default charset encoding.
	 * @return A stdout handler with the default charset.
	 */
	public static CharacterStreamWritingMessageHandler stdout() {
		return stdout(null);
	}

	/**
	 * Factory method that creates a target for stdout (System.out) with the
	 * specified charset encoding.
	 * @param charsetName The charset name.
	 * @return A stdout handler.
	 */
	public static CharacterStreamWritingMessageHandler stdout(@Nullable String charsetName) {
		return createTargetForStream(System.out, charsetName);
	}

	/**
	 * Factory method that creates a target for stderr (System.err) with the
	 * default charset encoding.
	 * @return A stderr handler with the default charset.
	 */
	public static CharacterStreamWritingMessageHandler stderr() {
		return stderr(null);
	}

	/**
	 * Factory method that creates a target for stderr (System.err) with the
	 * specified charset encoding.
	 * @param charsetName The charset name.
	 * @return A stderr handler.
	 */
	public static CharacterStreamWritingMessageHandler stderr(@Nullable String charsetName) {
		return createTargetForStream(System.err, charsetName);
	}

	private static CharacterStreamWritingMessageHandler createTargetForStream(OutputStream stream,
			@Nullable String charsetName) {

		if (charsetName == null) {
			return new CharacterStreamWritingMessageHandler(new OutputStreamWriter(stream));
		}
		try {
			return new CharacterStreamWritingMessageHandler(new OutputStreamWriter(stream, charsetName));
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("unsupported encoding: " + charsetName, e);
		}
	}

}
