/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.integration.gateway;

import java.lang.reflect.Method;

/**
 * Simple wrapper class containing a {@link Method} and an object
 * array containing the arguments for an invocation of that method.
 * For example, used by a {@link MethodArgsMessageMapper} with this generic
 * type to provide custom argument mapping when creating a message
 * in a {@code GatewayProxyFactoryBean}.
 *
 * @param method the method being invoked.
 * @param args the arguments for the method invocation.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public record MethodArgsHolder(Method method, Object[] args) {

	/**
	 * Return the method being invoked.
	 * @return the method being invoked.
	 * @deprecated since 7.0 in favor of {@link #method()}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the arguments for the method invocation.
	 * @return the arguments for the method invocation.
	 * @deprecated since 7.0 in favor of {@link #method()}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public Object[] getArgs() {
		return this.args;
	}

}
