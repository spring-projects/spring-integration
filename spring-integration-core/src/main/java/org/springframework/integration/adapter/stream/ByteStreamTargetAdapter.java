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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.adapter.AbstractTargetAdapter;

/**
 * A target adapter that writes a byte array to an {@link OutputStream}.
 * 
 * @author Mark Fisher
 */
public class ByteStreamTargetAdapter extends AbstractTargetAdapter {

	private BufferedOutputStream stream;


	public ByteStreamTargetAdapter(OutputStream stream) {
		this(stream, -1);
	}

	public ByteStreamTargetAdapter(OutputStream stream, int bufferSize) {
		if (bufferSize > 0) {
			this.stream = new BufferedOutputStream(stream, bufferSize);
		}
		else {
			this.stream = new BufferedOutputStream(stream);
		}
	}


	@Override
	protected boolean sendToTarget(Object object) {
		if (object == null) {
			if (logger.isWarnEnabled()) {
				logger.warn(this.getClass().getSimpleName() + " received null object");
			}
			return false;
		}
		try {
			if (object instanceof String) {
				this.stream.write(((String) object).getBytes());
			}
			else if (object instanceof byte[]){
				this.stream.write((byte[]) object);
			}
			else {
				throw new MessageHandlingException(this.getClass().getSimpleName() +
						" only supports byte array and String-based messages");
			}
			this.stream.flush();
			return true;
		}
		catch (IOException e) {
			throw new MessageHandlingException("IO failure occurred in adapter", e);
		}
	}

}
