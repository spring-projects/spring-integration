/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.stream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.ConfigurationException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Target;
import org.springframework.util.Assert;

/**
 * A target adapter that writes to a {@link Writer}. String-based
 * objects will be written directly, but if the object is not itself a
 * {@link String}, the adapter will write the result of the object's
 * {@link #toString()} method. To append a new-line after each write, set the
 * {@link #shouldAppendNewLine} flag to <em>true</em>. It is <em>false</em>
 * by default.
 * 
 * @author Mark Fisher
 */
public class CharacterStreamTargetAdapter implements Target {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final BufferedWriter writer;

	private volatile boolean shouldAppendNewLine = false;


	public CharacterStreamTargetAdapter(Writer writer) {
		this(writer, -1);
	}

	public CharacterStreamTargetAdapter(Writer writer, int bufferSize) {
		Assert.notNull(writer, "writer must not be null");
		if (writer instanceof BufferedWriter) {
			this.writer = (BufferedWriter) writer;
		}
		else if (bufferSize > 0) {
			this.writer = new BufferedWriter(writer, bufferSize);
		}
		else {
			this.writer = new BufferedWriter(writer);
		}
	}


	/**
	 * Factory method that creates an adapter for stdout (System.out) with the
	 * default charset encoding.
	 */
	public static CharacterStreamTargetAdapter stdoutAdapter() {
		return stdoutAdapter(null);
	}

	/**
	 * Factory method that creates an adapter for stdout (System.out) with the
	 * specified charset encoding.
	 */
	public static CharacterStreamTargetAdapter stdoutAdapter(String charsetName) {
		return createAdapterForStream(System.out, charsetName);
	}

	/**
	 * Factory method that creates an adapter for stderr (System.err) with the
	 * default charset encoding.
	 */
	public static CharacterStreamTargetAdapter stderrAdapter() {
		return stderrAdapter(null);
	}

	/**
	 * Factory method that creates an adapter for stderr (System.err) with the
	 * specified charset encoding.
	 */	
	public static CharacterStreamTargetAdapter stderrAdapter(String charsetName) {
		return createAdapterForStream(System.err, charsetName);
	}

	private static CharacterStreamTargetAdapter createAdapterForStream(OutputStream stream, String charsetName) {
		if (charsetName == null) {
			return new CharacterStreamTargetAdapter(new OutputStreamWriter(stream));
		}
		try {
			return new CharacterStreamTargetAdapter(new OutputStreamWriter(stream, charsetName));
		}
		catch (UnsupportedEncodingException e) {
			throw new ConfigurationException("unsupported encoding: " + charsetName, e);
		}
	}


	public void setShouldAppendNewLine(boolean shouldAppendNewLine) {
		this.shouldAppendNewLine = shouldAppendNewLine;
	}

	public boolean send(Message message) {
		Object payload = message.getPayload();
		if (payload == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("target adapter received null payload");
			}
			return false;
		}
		try {
			if (payload instanceof String) {
				writer.write((String) payload);
			}
			else if (payload instanceof char[]) {
				this.writer.write((char[]) payload);
			}
			else if (payload instanceof byte[]) {
				this.writer.write(new String((byte[]) payload));
			}
			else {
				writer.write(payload.toString());
			}
			if (this.shouldAppendNewLine) {
				writer.newLine();
			}
			writer.flush();
			return true;
		}
		catch (IOException e) {
			throw new MessagingException("IO failure occurred in adapter", e);
		}
	}

}
