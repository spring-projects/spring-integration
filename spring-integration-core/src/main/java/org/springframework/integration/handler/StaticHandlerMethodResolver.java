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

package org.springframework.integration.handler;

import java.lang.reflect.Method;

import org.springframework.integration.Message;
import org.springframework.util.Assert;

/**
 * An implementation of {@link HandlerMethodResolver} that always returns the
 * same Method instance. Used when the exact Method is indicated explicitly
 * or otherwise resolvable in advance based on static metadata.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class StaticHandlerMethodResolver implements HandlerMethodResolver {

	private final Method method;


	public StaticHandlerMethodResolver(Method method) {
		Assert.notNull(method, "method must not be null");
		Assert.isTrue(HandlerMethodUtils.isValidHandlerMethod(method),
				"Invalid Message-handling method [" + method + "]");
		this.method = method;
	}


	public Method resolveHandlerMethod(Message<?> message) {
		return this.method;
	}

}
