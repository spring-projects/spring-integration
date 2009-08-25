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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
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
		return this.getAnnotationValue(method, null, String.class);
	}

	public String[] getHeaderExpressions(Method method) {
		return this.getAnnotationValue(method, "headers", String[].class);
	}

	public String[] getArgumentNames(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			String name = annotation.argNames();
			if (StringUtils.hasText(name)) {
				return StringUtils.tokenizeToStringArray(name, ",");
			}
		}
		return this.parameterNameDiscoverer.getParameterNames(method);
	}

	public String getArgumentMapName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.argumentMapName();
		}
		return ExpressionSource.DEFAULT_ARGUMENT_MAP_NAME;
	}

	public String getReturnValueName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.returnValueName();
		}
		return ExpressionSource.DEFAULT_RETURN_VALUE_NAME;
	}

	public String getExceptionName(Method method) {
		ExpressionBinding annotation = AnnotationUtils.findAnnotation(method, ExpressionBinding.class);
		if (annotation != null) {
			return annotation.exceptionName();
		}
		return ExpressionSource.DEFAULT_EXCEPTION_NAME;		
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
