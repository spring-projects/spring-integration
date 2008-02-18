/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.integration.adapter.AbstractTargetAdapter;
import org.springframework.integration.message.MessageHandlingException;

/**
 * A target adapter that writes to an {@link OutputStream}. String-based
 * objects will be written directly, but if the object is not itself a
 * {@link String}, the adapter will write the result of the object's
 * {@link #toString()} method. To append a new-line after each write, set the
 * {@link #shouldAppendNewLine} flag to <em>true</em>. It is <em>false</em>
 * by default.
 * 
 * @author Mark Fisher
 */
public class CharacterStreamTargetAdapter extends AbstractTargetAdapter {

	private BufferedWriter writer;

	private boolean shouldAppendNewLine = false;


	public CharacterStreamTargetAdapter(OutputStream stream) {
		this(stream, -1);
	}

	public CharacterStreamTargetAdapter(OutputStream stream, int bufferSize) {
		if (bufferSize > 0) {
			this.writer = new BufferedWriter(new OutputStreamWriter(stream), bufferSize);
		}
		else {
			this.writer = new BufferedWriter(new OutputStreamWriter(stream));
		}
	}


	/**
	 * Factory method that creates an adapter for stdout (System.out).
	 */
	public static CharacterStreamTargetAdapter stdoutAdapter() {
		return new CharacterStreamTargetAdapter(System.out);
	}

	/**
	 * Factory method that creates an adapter for stderr (System.err).
	 */
	public static CharacterStreamTargetAdapter stderrAdapter() {
		return new CharacterStreamTargetAdapter(System.err);
	}


	public void setShouldAppendNewLine(boolean shouldAppendNewLine) {
		this.shouldAppendNewLine = shouldAppendNewLine;
	}

	@Override
	protected boolean sendToTarget(Object object) {
		if (object == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("target adapter received null object");
			}
			return false;
		}
		try {
			if (object instanceof String) {
				writer.write((String) object);
			}
			else {
				writer.write(object.toString());
			}
			if (this.shouldAppendNewLine) {
				writer.newLine();
			}
			writer.flush();
			return true;
		}
		catch (IOException e) {
			throw new MessageHandlingException("IO failure occurred in adapter", e);
		}
	}

}
