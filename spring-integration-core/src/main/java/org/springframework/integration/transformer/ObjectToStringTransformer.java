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

/**
 * A simple transformer that creates an outbound payload by invoking the
 * inbound payload Object's <code>toString()</code> method.
 * 
 * @author Mark Fisher
 * @since 1.0.1
 */
public class ObjectToStringTransformer extends AbstractPayloadTransformer<Object, String> {

	@Override
	protected String transformPayload(Object payload) throws Exception {
		if(payload instanceof byte[]) {
			return new String((byte[]) payload);
		}
		return payload.toString();
	}

}
