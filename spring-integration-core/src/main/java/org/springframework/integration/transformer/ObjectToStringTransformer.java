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

package org.springframework.integration.transformer;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import org.springframework.util.Assert;

/**
 * A simple transformer that creates an outbound payload by invoking the
 * inbound payload Object's <code>toString()</code> method. Unless the
 * payload is a <code>byte[]</code> or <code>char[]</code>. If the payload
 * is a byte[], it will be transformed to a String containing the
 * array's contents, using the {@link #charset}
 * which, by default, is "UTF-8". If the payload is a char[], it will be
 * transformed to a String object with the array's contents.
 *
 * @author Mark Fisher
 * @author Andrew Cowlin
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 1.0.1
 */
public class ObjectToStringTransformer extends AbstractPayloadTransformer<Object, String> {

	private final String charset;

	public ObjectToStringTransformer() {
		this(StandardCharsets.UTF_8.name());
	}

	public ObjectToStringTransformer(String charset) {
		Assert.notNull(charset, "'charset' cannot be null");
		this.charset = charset;
	}

	@Override
	public String getComponentType() {
		return "object-to-string-transformer";
	}

	@Override
	protected String transformPayload(Object payload) {
		if (payload instanceof byte[] bytes) {
			try {
				return new String(bytes, this.charset);
			}
			catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException(ex);
			}
		}
		else if (payload instanceof char[] chars) {
			return new String(chars);
		}
		else {
			return payload.toString();
		}
	}

}
