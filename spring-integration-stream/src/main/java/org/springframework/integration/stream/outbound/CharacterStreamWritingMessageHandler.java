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

package org.springframework.integration.stream.outbound;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

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
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class CharacterStreamWritingMessageHandler extends AbstractMessageHandler {

	private final BufferedWriter writer;

	private volatile boolean shouldAppendNewLine = false;

	public CharacterStreamWritingMessageHandler(Writer writer) {
		this(writer, -1);
	}

	public CharacterStreamWritingMessageHandler(Writer writer, int bufferSize) {
		Assert.notNull(writer, "writer must not be null");
		if (writer instanceof BufferedWriter bufferedWriter) {
			this.writer = bufferedWriter;
		}
		else if (bufferSize > 0) {
			this.writer = new BufferedWriter(writer, bufferSize);
		}
		else {
			this.writer = new BufferedWriter(writer);
		}
	}

	public void setShouldAppendNewLine(boolean shouldAppendNewLine) {
		this.shouldAppendNewLine = shouldAppendNewLine;
	}

	/**
	 * Fluent api for {@link #setShouldAppendNewLine(boolean)}.
	 * @param append true to append a newline.
	 * @return this.
	 */
	public CharacterStreamWritingMessageHandler appendNewLine(boolean append) {
		setShouldAppendNewLine(append);
		return this;
	}

	@Override
	public String getComponentType() {
		return "stream:outbound-channel-adapter(character)";
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object payload = message.getPayload();
		try {
			if (payload instanceof String string) {
				this.writer.write(string);
			}
			else if (payload instanceof char[] chars) {
				this.writer.write(chars);
			}
			else if (payload instanceof byte[] bytes) {
				this.writer.write(new String(bytes));
			}
			else if (payload instanceof Exception exception) {
				PrintWriter printWriter = new PrintWriter(this.writer, true);
				exception.printStackTrace(printWriter);
			}
			else {
				this.writer.write(payload.toString());
			}
			if (this.shouldAppendNewLine) {
				this.writer.newLine();
			}
			this.writer.flush();
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in target", e);
		}
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
