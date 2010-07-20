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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Payload;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link ExpressionSource} implementation that retrieves the expression
 * string and evaluation context variable names from an annotation.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class MethodAnnotationExpressionSource implements ExpressionSource {

	private final Set<Class<? extends Annotation>> annotationTypes;

	private volatile String channelAttributeName = "channel";

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();


	public MethodAnnotationExpressionSource() {
		this(Collections.<Class<? extends Annotation>>singleton(Publisher.class));
	}

	public MethodAnnotationExpressionSource(Set<Class<? extends Annotation>> annotationTypes) {
		Assert.notEmpty(annotationTypes, "annotationTypes must not be empty");
		this.annotationTypes = annotationTypes;
	}


	public void setChannelAttributeName(String channelAttributeName) {
		Assert.hasText(channelAttributeName, "channelAttributeName must not be empty");
		this.channelAttributeName = channelAttributeName;
	}

	public String getPayloadExpression(Method method) {
		String payloadExpression = null;
		method.getAnnotation(Payload.class);
		Payload methodPayloadAnnotation = AnnotationUtils.findAnnotation(method, Payload.class);
		if (methodPayloadAnnotation != null) {
			payloadExpression = StringUtils.hasText(methodPayloadAnnotation.value())
					? methodPayloadAnnotation.value()
					: "#" + this.getReturnValueVariableName(method);
		}
		Annotation[][] annotationArray = method.getParameterAnnotations();
		for (int i = 0; i < annotationArray.length; i++) {
			Annotation[] parameterAnnotations = annotationArray[i];
			for (Annotation currentAnnotation : parameterAnnotations) {
				if (Payload.class.equals(currentAnnotation.annotationType())) {
					Assert.state(payloadExpression == null,
							"@Payload can be used at most once on a @Publisher method, either at method-level or on a single parameter");
					Assert.state("".equals(((Payload) currentAnnotation).value()),
							"@Payload on a parameter for a @Publisher method may not contain an expression");
					payloadExpression = "#" + this.getArgumentMapVariableName(method) + "[" + i + "]";
				}
			}
		}
		return payloadExpression;
	}

	public Map<String, String> getHeaderExpressions(Method method) {
		Map<String, String> headerExpressions = new HashMap<String, String>();
		String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
		Annotation[][] annotationArray = method.getParameterAnnotations();
		for (int i = 0; i < annotationArray.length; i++) {
			Annotation[] parameterAnnotations = annotationArray[i];
			for (Annotation currentAnnotation : parameterAnnotations) {
				if (Header.class.equals(currentAnnotation.annotationType())) {
					Header headerAnnotation = (Header) currentAnnotation;
					String name = headerAnnotation.value();
					if (!StringUtils.hasText(name)) {
						name = parameterNames[i];
					}
					headerExpressions.put(name, "#" + this.getArgumentMapVariableName(method) + "['" + i + "']");
				}
			}
		}
		return headerExpressions;
	}

	public String getMethodNameVariableName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.methodNameVariableName();
		}
		return ExpressionSource.DEFAULT_METHOD_NAME_VARIABLE_NAME;
	}

	public String[] getArgumentVariableNames(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			String argNameList = annotation.argumentVariableNames();
			if (StringUtils.hasText(argNameList)) {
				return StringUtils.tokenizeToStringArray(argNameList, ",");
			}
		}
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	public String getArgumentMapVariableName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.argumentMapVariableName();
		}
		return ExpressionSource.DEFAULT_ARGUMENT_MAP_VARIABLE_NAME;
	}

	public String getReturnValueVariableName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.returnValueVariableName();
		}
		return ExpressionSource.DEFAULT_RETURN_VALUE_VARIABLE_NAME;
	}

	public String getExceptionVariableName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.exceptionVariableName();
		}
		return ExpressionSource.DEFAULT_EXCEPTION_VARIABLE_NAME;		
	}

	public String getChannelName(Method method) {
		String channelName = this.getAnnotationValue(method, this.channelAttributeName, String.class);
		return (StringUtils.hasText(channelName) ? channelName : null);
	}

	@SuppressWarnings("unchecked")
	private <T> T getAnnotationValue(Method method, String attributeName, Class<T> expectedType) {
		T value = null;
		for (Class<? extends Annotation> annotationType : this.annotationTypes) {
			Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
			if (annotation != null) {
				if (value != null) {
					throw new IllegalStateException(
							"method [" + method + "] contains more than one publisher annotation");
				}
				Object valueAsObject = (attributeName == null) ?  AnnotationUtils.getValue(annotation)
						: AnnotationUtils.getValue(annotation, attributeName);
				if (valueAsObject != null) {
					if (expectedType.isAssignableFrom(valueAsObject.getClass())) {
						value = (T) valueAsObject;
					}
					else {
						throw new IllegalArgumentException("expected type [" + expectedType.getName() +
								"] for attribute '" + attributeName + "' on publisher annotation [" +
								annotationType + "], but actual type was [" + valueAsObject.getClass() + "]");
					}
				}
			}
		}
		return value;
	}

}
