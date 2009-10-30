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
import java.util.Collections;
import java.util.Map;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodNameMappingExpressionSource implements ExpressionSource {

	private final Map<String, String> payloadExpressionMap;

	private volatile Map<String, String[]> headerExpressionMap = Collections.emptyMap();

	private volatile Map<String, String> channelMap = Collections.emptyMap();

	private volatile Map<String, String[]> argumentVariableNameMap;

	private volatile String methodNameVariableName = ExpressionSource.DEFAULT_METHOD_NAME_VARIABLE_NAME;

	private volatile String argumentMapVariableName = ExpressionSource.DEFAULT_ARGUMENT_MAP_VARIABLE_NAME;

	private volatile String returnValueVariableName = ExpressionSource.DEFAULT_RETURN_VALUE_VARIABLE_NAME;

	private volatile String exceptionVariableName = ExpressionSource.DEFAULT_EXCEPTION_VARIABLE_NAME;

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();


	public MethodNameMappingExpressionSource(Map<String, String> payloadExpressionMap) {
		Assert.notEmpty(payloadExpressionMap, "payloadExpressionMap must not be empty");
		this.payloadExpressionMap = payloadExpressionMap;
	}


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

	public void setArgumentVariableNameMap(Map<String, String[]> argumentVariableNameMap) {
		this.argumentVariableNameMap = argumentVariableNameMap;
	}

	public void setHeaderExpressionMap(Map<String, String[]> headerExpressionMap) {
		this.headerExpressionMap = headerExpressionMap;
	}

	public void setChannelMap(Map<String, String> channelMap) {
		this.channelMap = channelMap;
	}

	public String[] getArgumentVariableNames(Method method) {
		if (this.argumentVariableNameMap != null) {
			for (Map.Entry<String, String[]> entry : this.argumentVariableNameMap.entrySet()) {
				if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
					return entry.getValue();
				}
			}
		}
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	public String getPayloadExpression(Method method) {
		for (Map.Entry<String, String> entry : this.payloadExpressionMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public String[] getHeaderExpressions(Method method) {
		for (Map.Entry<String, String[]> entry : this.headerExpressionMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

	public String getChannelName(Method method) {
		for (Map.Entry<String, String> entry : this.channelMap.entrySet()) {
			if (PatternMatchUtils.simpleMatch(entry.getKey(), method.getName())) {
				return entry.getValue();
			}
		}
		return null;
	}

}
