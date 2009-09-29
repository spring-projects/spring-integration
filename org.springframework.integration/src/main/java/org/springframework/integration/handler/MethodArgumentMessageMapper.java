/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.handler;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MethodParameterMessageMapper;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodArgumentMessageMapper implements OutboundMessageMapper<Object[]> {

	private final Method method;

	//TODO: move core mapping logic from MethodParameterMessageMapper into this class
	private final MethodParameterMessageMapper innerMapper;


	public MethodArgumentMessageMapper(Method method) {
		Assert.notNull(method, "method must not be null");
		this.method = method;
		this.innerMapper = new MethodParameterMessageMapper(method);
	}


	public Object[] fromMessage(Message<?> message) throws Exception {
		Object args[] = null;
		Object mappingResult = this.innerMapper.fromMessage(message);
		if (mappingResult != null && mappingResult.getClass().isArray()
				&& (Object.class.isAssignableFrom(mappingResult.getClass().getComponentType()))) {
			args = (Object[]) mappingResult;
		}
		else {
			args = new Object[] { mappingResult };
		}
		if (args.length > 1 && message != null && message.getPayload() instanceof Map) {
			int mapArgCount = 0;
			boolean resolvedMapArg = false;
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				if (arg instanceof Map && Map.class.isAssignableFrom(method.getParameterTypes()[i])) {
					mapArgCount++;
					if (arg.equals(message.getPayload())) {
						// resolved if there is exactly one match
						resolvedMapArg = !resolvedMapArg;
					}
				}
			}
			Assert.isTrue(resolvedMapArg || mapArgCount <= 1,
					"Unable to resolve argument for Map-typed payload on method [" + method + "].");
		}
		return args;
	}

}
