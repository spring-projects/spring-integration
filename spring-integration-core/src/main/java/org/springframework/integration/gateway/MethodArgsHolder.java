/*
 * Copyright 2013-2019 the original author or authors.
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
 * For example used by a {@link MethodArgsMessageMapper} with this generic
 * type to provide custom argument mapping when creating a message
 * in a {@code GatewayProxyFactoryBean}.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public final class MethodArgsHolder {

	private final Method method;

	private final Object[] args;

	public MethodArgsHolder(Method method, Object[] args) { //NOSONAR - direct storage
		this.method = method;
		this.args = args; //NOSONAR - direct storage
	}

	public Method getMethod() {
		return this.method;
	}

	public Object[] getArgs() {
		return this.args; //NOSONAR - direct access
	}

}
