/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Strategy for determining the channel name, payload expression, and header expressions
 * for the {@link MessagePublishingInterceptor}.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
interface PublisherMetadataSource {

	static final String METHOD_NAME_VARIABLE_NAME = "method";

	static final String ARGUMENT_MAP_VARIABLE_NAME = "args";

	static final String RETURN_VALUE_VARIABLE_NAME = "return";

	static final String EXCEPTION_VARIABLE_NAME = "exception";


	/**
	 * Returns the channel name to which Messages should be published
	 * for this particular method invocation.
	 *
	 * @param method The Method.
	 * @return The channel name.
	 */
	String getChannelName(Method method);

	/**
	 * Returns the expression string to be evaluated for creating the Message
	 * payload.
	 *
	 * @param method The Method.
	 * @return The payload expression.
	 */
	String getPayloadExpression(Method method);

	/**
	 * Returns the map of expression strings to be evaluated for any headers
	 * that should be set on the published Message. The keys in the Map are
	 * header names, the values are the expression strings.
	 *
	 * @param method The Method.
	 * @return The header expressions.
	 */
	Map<String, String> getHeaderExpressions(Method method);

}
