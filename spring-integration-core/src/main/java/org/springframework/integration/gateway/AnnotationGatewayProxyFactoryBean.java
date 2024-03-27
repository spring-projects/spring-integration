/*
 * Copyright 2017-2024 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.JavaUtils;
import org.springframework.integration.annotation.AnnotationConstants;
import org.springframework.integration.annotation.GatewayHeader;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.util.MessagingAnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link GatewayProxyFactoryBean} extension for Java configuration.
 * The service interface may be marked with the {@link MessagingGateway} annotation.
 * Otherwise, the default state is applied.
 *
 * @param <T> the target gateway interface to build a proxy against.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class AnnotationGatewayProxyFactoryBean<T> extends GatewayProxyFactoryBean<T> {

	private final AnnotationAttributes gatewayAttributes;

	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

	private BeanExpressionContext expressionContext;

	public AnnotationGatewayProxyFactoryBean(Class<T> serviceInterface) {
		super(serviceInterface);
		this.gatewayAttributes = mergeAnnotationAttributes(serviceInterface);
	}

	private static AnnotationAttributes mergeAnnotationAttributes(Class<?> serviceInterface) {
		AnnotationAttributes annotationAttributes =
				AnnotationUtils.getAnnotationAttributes(null,
						AnnotationUtils.synthesizeAnnotation(MessagingGateway.class));

		if (AnnotatedElementUtils.isAnnotated(serviceInterface, MessagingGateway.class)) {
			Annotation annotation =
					MergedAnnotations.from(serviceInterface)
							.get(MessagingGateway.class)
							.getRoot()
							.synthesize();

			List<Annotation> annotationChain =
					MessagingAnnotationUtils.getAnnotationChain(annotation, MessagingGateway.class);

			for (Map.Entry<String, Object> attribute : annotationAttributes.entrySet()) {
				String key = attribute.getKey();
				Object value = MessagingAnnotationUtils.resolveAttribute(annotationChain, key, Object.class);
				if (value != null) {
					attribute.setValue(value);
				}
			}
		}

		return annotationAttributes;
	}

	@Override
	protected void onInit() {
		if (getBeanFactory() instanceof ConfigurableBeanFactory beanFactory) {
			this.resolver = beanFactory.getBeanExpressionResolver();
			this.expressionContext = new BeanExpressionContext(beanFactory, null);
		}

		populateGatewayMethodMetadataIfAny();

		String defaultRequestTimeout = resolveAttribute("defaultRequestTimeout");
		String defaultReplyTimeout = resolveAttribute("defaultReplyTimeout");

		JavaUtils.INSTANCE
				.acceptIfCondition(getDefaultRequestChannel() == null && getDefaultRequestChannelName() == null,
						resolveAttribute("defaultRequestChannel"),
						this::setDefaultRequestChannelName)
				.acceptIfCondition(getDefaultReplyChannel() == null && getDefaultReplyChannelName() == null,
						resolveAttribute("defaultReplyChannel"),
						this::setDefaultReplyChannelName)
				.acceptIfCondition(getErrorChannel() == null && getErrorChannelName() == null,
						resolveAttribute("errorChannel"),
						this::setErrorChannelName)
				.acceptIfCondition(getDefaultRequestTimeout() == null && StringUtils.hasText(defaultRequestTimeout),
						evaluateExpression(defaultRequestTimeout, Long.class),
						this::setDefaultRequestTimeout)
				.acceptIfCondition(getDefaultReplyTimeout() == null && StringUtils.hasText(defaultReplyTimeout),
						evaluateExpression(defaultReplyTimeout, Long.class),
						this::setDefaultReplyTimeout);

		populateAsyncExecutorIfAny();

		setErrorOnTimeout(this.gatewayAttributes.getBoolean("errorOnTimeout"));

		boolean proxyDefaultMethods = this.gatewayAttributes.getBoolean("proxyDefaultMethods");
		if (proxyDefaultMethods) { // Override only if annotation attribute is different
			setProxyDefaultMethods(true);
		}
		super.onInit();
	}

	private void populateGatewayMethodMetadataIfAny() {
		if (getGlobalMethodMetadata() != null) {
			return;
		}

		ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) getBeanFactory();

		String defaultPayloadExpression = resolveAttribute("defaultPayloadExpression");

		GatewayHeader[] defaultHeaders = (GatewayHeader[]) this.gatewayAttributes.get("defaultHeaders");

		String mapper = resolveAttribute("mapper");

		boolean hasMapper = StringUtils.hasText(mapper);
		boolean hasDefaultPayloadExpression = StringUtils.hasText(defaultPayloadExpression);
		Assert.state(!hasMapper || !hasDefaultPayloadExpression,
				"'defaultPayloadExpression' is not allowed when a 'mapper' is provided");

		boolean hasDefaultHeaders = !ObjectUtils.isEmpty(defaultHeaders);
		Assert.state(!hasMapper || !hasDefaultHeaders,
				"'defaultHeaders' are not allowed when a 'mapper' is provided");

		JavaUtils.INSTANCE
				.acceptIfCondition(hasMapper && getMapper() == null, mapper,
						value -> setMapper(beanFactory.getBean(value, MethodArgsMessageMapper.class)));

		if (hasDefaultHeaders || hasDefaultPayloadExpression) {
			GatewayMethodMetadata gatewayMethodMetadata = new GatewayMethodMetadata();

			if (hasDefaultPayloadExpression) {
				gatewayMethodMetadata.setPayloadExpression(EXPRESSION_PARSER.parseExpression(defaultPayloadExpression));
			}

			Map<String, Expression> headerExpressions =
					Arrays.stream(defaultHeaders)
							.collect(Collectors.toMap(
									header -> beanFactory.resolveEmbeddedValue(header.name()),
									header -> {
										String headerValue =
												beanFactory.resolveEmbeddedValue(header.value());
										boolean hasValue = StringUtils.hasText(headerValue);

										String headerExpression =
												beanFactory.resolveEmbeddedValue(header.expression());

										Assert.state(!(hasValue == StringUtils.hasText(headerExpression)),
												"exactly one of 'value' or 'expression' is required on a gateway's " +
														"header.");

										return hasValue ?
												new LiteralExpression(headerValue) :
												EXPRESSION_PARSER.parseExpression(headerExpression);
									}));

			gatewayMethodMetadata.setHeaderExpressions(headerExpressions);

			setGlobalMethodMetadata(gatewayMethodMetadata);
		}
	}

	private void populateAsyncExecutorIfAny() {
		BeanFactory beanFactory = getBeanFactory();
		if (!isAsyncExecutorExplicitlySet()) {
			String asyncExecutor = resolveAttribute("asyncExecutor");
			if (asyncExecutor == null || AnnotationConstants.NULL.equals(asyncExecutor)) {
				setAsyncExecutor(null);
			}
			else if (StringUtils.hasText(asyncExecutor)) {
				setAsyncExecutor(beanFactory.getBean(asyncExecutor, Executor.class));
			}
		}
	}

	@Nullable
	private String resolveAttribute(String attributeName) {
		ConfigurableBeanFactory beanFactory = (ConfigurableBeanFactory) getBeanFactory();
		return beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString(attributeName));
	}

	@Nullable
	private <V> V evaluateExpression(@Nullable String value, Class<V> targetClass) {
		if (StringUtils.hasText(value)) {
			Object result = this.resolver.evaluate(value, this.expressionContext);
			return getConversionService().convert(result, targetClass);
		}
		else {
			return null;
		}
	}

}
