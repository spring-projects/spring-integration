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

package org.springframework.integration.transformer;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;

/**
 * Transformer that deserializes the inbound byte array payload to an object.
 * <p>The byte array payload must be a result of serialization.
 * 
 * @author Mark Fisher
 * @since 1.0.1
 */
public class PayloadDeserializingTransformer extends AbstractPayloadTransformer<byte[], Object> {

	@Override
	protected Object transformPayload(byte[] payload) throws Exception {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(payload);
		ObjectInputStream objectStream = null;
		try {
			objectStream = new ObjectInputStream(byteStream);
			return objectStream.readObject();
		}
		catch (ObjectStreamException e) {
			throw new IllegalArgumentException(
					"Failed to deserialize payload. Is the byte array a result of Object serialization?", e);
		}
		finally {
			try {
				objectStream.close();
			}
			catch (Exception e) {
				// ignore
			}
		}
	}

}
