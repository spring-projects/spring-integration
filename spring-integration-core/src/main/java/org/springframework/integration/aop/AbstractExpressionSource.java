/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Base class for {@link ExpressionSource} implementations.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractExpressionSource implements ExpressionSource {

	private volatile String methodNameVariableName = ExpressionSource.DEFAULT_METHOD_NAME_VARIABLE_NAME;

	private volatile String argumentMapVariableName = ExpressionSource.DEFAULT_ARGUMENT_MAP_VARIABLE_NAME;

	private volatile String returnValueVariableName = ExpressionSource.DEFAULT_RETURN_VALUE_VARIABLE_NAME;

	private volatile String exceptionVariableName = ExpressionSource.DEFAULT_EXCEPTION_VARIABLE_NAME;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();


	public void setMethodNameVariableName(String methodNameVariableName) {
		this.methodNameVariableName = methodNameVariableName;
	}

	public String getMethodNameVariableName(Method method) {
		return this.methodNameVariableName;
	}

	public void setArgumentMapVariableName(String argumentMapVariableName) {
		this.argumentMapVariableName = argumentMapVariableName;
	}

	public String getArgumentMapVariableName(Method method) {
		return this.argumentMapVariableName;
	}

	public void setExceptionVariableName(String exceptionVariableName) {
		this.exceptionVariableName = exceptionVariableName;
	}

	public String getExceptionVariableName(Method method) {
		return this.exceptionVariableName;
	}

	public void setReturnValueVariableName(String returnValueVariableName) {
		this.returnValueVariableName = returnValueVariableName;
	}

	public String getReturnValueVariableName(Method method) {
		return this.returnValueVariableName;
	}

	protected String[] discoverMethodParameterNames(Method method) {
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	public abstract String getPayloadExpression(Method method);

	public abstract String[] getArgumentVariableNames(Method method);

	public abstract Map<String, String> getHeaderExpressions(Method method);

	public abstract String getChannelName(Method method);

}
