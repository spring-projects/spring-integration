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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.integration.annotation.Publisher;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * An {@link PublisherMetadataSource} implementation that retrieves the channel
 * name and expression strings from an annotation.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class MethodAnnotationPublisherMetadataSource implements PublisherMetadataSource {

	private final Set<Class<? extends Annotation>> annotationTypes;

	private volatile String channelAttributeName = "channel";

	private final ParameterNameDiscoverer parameterNameDiscoverer = new LocalVariableTableParameterNameDiscoverer();


	public MethodAnnotationPublisherMetadataSource() {
		this(Collections.<Class<? extends Annotation>>singleton(Publisher.class));
	}

	public MethodAnnotationPublisherMetadataSource(Set<Class<? extends Annotation>> annotationTypes) {
		Assert.notEmpty(annotationTypes, "annotationTypes must not be empty");
		this.annotationTypes = annotationTypes;
	}


	public void setChannelAttributeName(String channelAttributeName) {
		Assert.hasText(channelAttributeName, "channelAttributeName must not be empty");
		this.channelAttributeName = channelAttributeName;
	}

	public String getChannelName(Method method) {
		String channelName = this.getAnnotationValue(method, this.channelAttributeName, String.class);
		if (channelName == null) {
			channelName = this.getAnnotationValue(method.getDeclaringClass(), this.channelAttributeName, String.class);
		}
		return (StringUtils.hasText(channelName) ? channelName : null);
	}

	@SuppressWarnings("deprecation")
	public String getPayloadExpression(Method method) {
		String payloadExpression = null;
		Annotation methodPayloadAnnotation =
				AnnotationUtils.findAnnotation(method, org.springframework.integration.annotation.Payload.class);
		if (methodPayloadAnnotation == null) {
			methodPayloadAnnotation = AnnotationUtils.findAnnotation(method, Payload.class);
		}

		if (methodPayloadAnnotation != null) {
			payloadExpression = getAnnotationValue(methodPayloadAnnotation, null, String.class);
			if (!StringUtils.hasText(payloadExpression)) {
				payloadExpression = "#" + PublisherMetadataSource.RETURN_VALUE_VARIABLE_NAME;
			}
		}

		Annotation[][] annotationArray = method.getParameterAnnotations();
		for (int i = 0; i < annotationArray.length; i++) {
			Annotation[] parameterAnnotations = annotationArray[i];
			for (Annotation currentAnnotation : parameterAnnotations) {
				if (org.springframework.integration.annotation.Payload.class.equals(currentAnnotation.annotationType())
						|| Payload.class.equals(currentAnnotation.annotationType())) {
					Assert.state(payloadExpression == null,
							"@Payload can be used at most once on a @Publisher method, " +
									"either at method-level or on a single parameter");
					Assert.state("".equals(AnnotationUtils.getValue(currentAnnotation)),
							"@Payload on a parameter for a @Publisher method may not contain an expression");
					payloadExpression = "#" + PublisherMetadataSource.ARGUMENT_MAP_VARIABLE_NAME + "[" + i + "]";
				}
			}
		}
		if (payloadExpression == null
				|| payloadExpression.contains("#" + PublisherMetadataSource.RETURN_VALUE_VARIABLE_NAME)) {
			Assert.isTrue(!void.class.equals(method.getReturnType()),
					"When defining @Publisher on a void-returning method, an explicit payload " +
					"expression that does not rely upon a #return value is required.");
		}
		return payloadExpression;
	}

	@SuppressWarnings("deprecation")
	public Map<String, String> getHeaderExpressions(Method method) {
		Map<String, String> headerExpressions = new HashMap<String, String>();
		String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
		Annotation[][] annotationArray = method.getParameterAnnotations();
		for (int i = 0; i < annotationArray.length; i++) {
			Annotation[] parameterAnnotations = annotationArray[i];
			for (Annotation currentAnnotation : parameterAnnotations) {
				if (org.springframework.integration.annotation.Header.class.equals(currentAnnotation.annotationType())
						|| Header.class.equals(currentAnnotation.annotationType())) {
					String name = getAnnotationValue(currentAnnotation, null, String.class);
					if (!StringUtils.hasText(name)) {
						name = parameterNames[i];
					}
					headerExpressions.put(name,
							"#" + PublisherMetadataSource.ARGUMENT_MAP_VARIABLE_NAME + "[" + i + "]");
				}
			}
		}
		return headerExpressions;
	}

	private <T> T getAnnotationValue(Method method, String attributeName, Class<T> expectedType) {
		T value = null;
		for (Class<? extends Annotation> annotationType : this.annotationTypes) {
			Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
			if (annotation != null) {
				if (value != null) {
					throw new IllegalStateException(
							"method [" + method + "] contains more than one publisher annotation");
				}
				value = this.getAnnotationValue(annotation, attributeName, expectedType);
			}
		}
		return value;
	}

	private <T> T getAnnotationValue(Class<?> clazz, String attributeName, Class<T> expectedType) {
		T value = null;
		for (Class<? extends Annotation> annotationType : this.annotationTypes) {
			Annotation annotation = AnnotationUtils.findAnnotation(clazz, annotationType);
			if (annotation != null) {
				if (value != null) {
					throw new IllegalStateException(
							"class [" + clazz + "] contains more than one publisher annotation");
				}
				value = this.getAnnotationValue(annotation, attributeName, expectedType);
			}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	private <T> T getAnnotationValue(Annotation annotation, String attributeName, Class<T> expectedType) {
		T value = null;
		Object valueAsObject = (attributeName == null) ?  AnnotationUtils.getValue(annotation)
				: AnnotationUtils.getValue(annotation, attributeName);
		if (valueAsObject != null) {
			if (expectedType.isAssignableFrom(valueAsObject.getClass())) {
				value = (T) valueAsObject;
			}
			else {
				throw new IllegalArgumentException("expected type [" + expectedType.getName() +
						"] for attribute '" + attributeName + "' on publisher annotation [" +
						annotation.annotationType() + "], but actual type was [" + valueAsObject.getClass() + "]");
			}
		}
		return value;
	}

}
