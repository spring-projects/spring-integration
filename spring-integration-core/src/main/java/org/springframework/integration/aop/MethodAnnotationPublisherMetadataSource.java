/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.expression.Expression;
import org.springframework.integration.annotation.Publisher;
import org.springframework.lang.Nullable;
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
 * @author Gareth Chapman
 * @author Cameron Mayfield
 * @author Chengchen Ji
 * @author Gary Russell
 *
 * @since 2.0
 */
public class MethodAnnotationPublisherMetadataSource implements PublisherMetadataSource {

	private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

	private final Map<Method, String> channels = new ConcurrentHashMap<>();

	private final Map<Method, Expression> payloadExpressions = new ConcurrentHashMap<>();

	private final Map<Method, Map<String, Expression>> headersExpressions = new ConcurrentHashMap<>();

	private final Set<Class<? extends Annotation>> annotationTypes;

	private volatile String channelAttributeName = "channel";

	public MethodAnnotationPublisherMetadataSource() {
		this(Collections.singleton(Publisher.class));
	}

	public MethodAnnotationPublisherMetadataSource(Set<Class<? extends Annotation>> annotationTypes) {
		Assert.notEmpty(annotationTypes, "annotationTypes must not be empty");
		this.annotationTypes = annotationTypes;
	}

	public void setChannelAttributeName(String channelAttributeName) {
		Assert.hasText(channelAttributeName, "channelAttributeName must not be empty");
		this.channelAttributeName = channelAttributeName;
	}

	@Override
	public String getChannelName(Method method) {
		return this.channels.computeIfAbsent(method,
				method1 -> {
					String channelName = getAnnotationValue(method, this.channelAttributeName);
					if (channelName == null) {
						channelName = getAnnotationValue(method.getDeclaringClass(), this.channelAttributeName);
					}
					return StringUtils.hasText(channelName) ? channelName : null;
				});
	}

	@Override
	public Expression getExpressionForPayload(Method method) {
		return this.payloadExpressions.computeIfAbsent(method,
				method1 -> {
					Expression payloadExpression = null;
					MergedAnnotation<Payload> payloadMergedAnnotation =
							MergedAnnotations.from(method, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
									.get(Payload.class);
					if (payloadMergedAnnotation.isPresent()) {
						String payloadExpressionString = payloadMergedAnnotation.getString("expression");
						if (!StringUtils.hasText(payloadExpressionString)) {
							payloadExpression = RETURN_VALUE_EXPRESSION;
						}
						else {
							payloadExpression = EXPRESSION_PARSER.parseExpression(payloadExpressionString);
						}
					}

					Annotation[][] annotationArray = method.getParameterAnnotations();
					for (int i = 0; i < annotationArray.length; i++) {
						Annotation[] parameterAnnotations = annotationArray[i];
						payloadMergedAnnotation = MergedAnnotations.from(parameterAnnotations).get(Payload.class);
						if (payloadMergedAnnotation.isPresent()) {
							Assert.state(payloadExpression == null,
									"@Payload can be used at most once on a @Publisher method, " +
											"either at method-level or on a single parameter");

							Assert.state("".equals(payloadMergedAnnotation.getString("expression")),
									"@Payload on a parameter for a @Publisher method may not contain an 'expression'");

							payloadExpression =
									EXPRESSION_PARSER.parseExpression("#" + ARGUMENT_MAP_VARIABLE_NAME + "[" + i + "]");
						}
					}
					if (payloadExpression == null ||
							RETURN_VALUE_EXPRESSION.getExpressionString()
									.equals(payloadExpression.getExpressionString())) {
						Assert.isTrue(!void.class.equals(method.getReturnType()),
								"When defining @Publisher on a void-returning method, an explicit payload " +
										"expression that does not rely upon a #return value is required.");
					}
					return payloadExpression;
				});
	}

	@Override
	public Map<String, Expression> getExpressionsForHeaders(Method method) {
		return this.headersExpressions.computeIfAbsent(method,
				method1 -> {
					Map<String, Expression> headerExpressions = new HashMap<>();
					String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(method);
					Annotation[][] annotationArray = method.getParameterAnnotations();
					for (int i = 0; i < annotationArray.length; i++) {
						Annotation[] parameterAnnotations = annotationArray[i];
						MergedAnnotation<Header> headerMergedAnnotation =
								MergedAnnotations.from(parameterAnnotations).get(Header.class);
						if (headerMergedAnnotation.isPresent()) {
							String name =
									headerMergedAnnotation
											.getString("name");
							if (!StringUtils.hasText(name)) {
								if (parameterNames != null) {
									name = parameterNames[i];
								}
								else {
									name = method.getName() + ".arg#" + i;
								}
							}
							headerExpressions.put(name,
									EXPRESSION_PARSER.parseExpression('#' + ARGUMENT_MAP_VARIABLE_NAME + '[' + i + ']'));
						}
					}
					return headerExpressions;
				});
	}

	@Nullable
	private String getAnnotationValue(AnnotatedElement element, String attributeName) {
		MergedAnnotations mergedAnnotations =
				MergedAnnotations.from(element, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
		String value = null;
		for (Class<? extends Annotation> annotationType : this.annotationTypes) {
			MergedAnnotation<? extends Annotation> mergedAnnotation = mergedAnnotations.get(annotationType);
			if (mergedAnnotation.isPresent()) {
				if (value != null) {
					throw new IllegalStateException(
							"The [" + element + "] contains more than one publisher annotation");
				}
				value = mergedAnnotation.getString(attributeName);
			}
		}
		return value;
	}

}
