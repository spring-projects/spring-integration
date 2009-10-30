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

package org.springframework.integration.aop;

import java.lang.reflect.Method;

/**
 * Strategy for determining the expression string and evaluation context
 * variable names from a Method.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
interface ExpressionSource {

	static final String DEFAULT_METHOD_NAME_VARIABLE_NAME = "method";

	static final String DEFAULT_ARGUMENT_MAP_VARIABLE_NAME = "args";

	static final String DEFAULT_RETURN_VALUE_VARIABLE_NAME = "return";

	static final String DEFAULT_EXCEPTION_VARIABLE_NAME = "exception";


	/**
	 * Returns the expression string to be evaluated for creating the Message
	 * payload.
	 */
	String getPayloadExpression(Method method);

	/**
	 * Returns the array of expression strings to be evaluated for any headers
	 * that should be set on the published Message.
	 */
	String[] getHeaderExpressions(Method method);

	/**
	 * Returns the variable name to be associated with the intercepted
	 * method's name.
	 */
	String getMethodNameVariableName(Method method);

	/**
	 * Returns the variable names to be associated with the intercepted method
	 * invocation's argument array.
	 */
	String[] getArgumentVariableNames(Method method);

	/**
	 * Returns the variable name to use in the evaluation context for the Map
	 * of arguments. The keys in this map will be determined by the result of
	 * the {@link #getArgumentNames(Method)} method.
	 */
	String getArgumentMapVariableName(Method method);

	/**
	 * Returns the variable name to use in the evaluation context for any
	 * return value resulting from the method invocation.
	 */
	String getReturnValueVariableName(Method method);

	/**
	 * Returns the variable name to use in the evaluation context for any
	 * exception thrown from the method invocation.
	 */
	String getExceptionVariableName(Method method);

	/**
	 * Returns the channel name to which Messages should be published
	 * for this particular method invocation.
	 */
	String getChannelName(Method method);

}
