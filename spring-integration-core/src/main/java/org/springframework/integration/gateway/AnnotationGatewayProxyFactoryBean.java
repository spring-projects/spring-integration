/*
 * Copyright 2017-2020 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.AnnotationConstants;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.util.JavaUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link GatewayProxyFactoryBean} extension for Java configuration.
 * The service interface may be marked with the {@link MessagingGateway} annotation.
 * Otherwise the default state is applied.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class AnnotationGatewayProxyFactoryBean extends GatewayProxyFactoryBean {

	private final AnnotationAttributes gatewayAttributes;

	public AnnotationGatewayProxyFactoryBean(Class<?> serviceInterface) {
		super(serviceInterface);

		AnnotationAttributes annotationAttributes =
				AnnotatedElementUtils.getMergedAnnotationAttributes(serviceInterface,
						MessagingGateway.class.getName(), false, true);
		if (annotationAttributes == null) {
			annotationAttributes = AnnotationUtils.getAnnotationAttributes(
					AnnotationUtils.synthesizeAnnotation(MessagingGateway.class), false, true);
		}

		this.gatewayAttributes = annotationAttributes;

		String id = annotationAttributes.getString("name");
		if (StringUtils.hasText(id)) {
			setBeanName(id);
		}
	}

	@Override
	protected void onInit() {
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
						defaultRequestTimeout,
						value -> setDefaultRequestTimeout(Long.parseLong(value)))
				.acceptIfCondition(getDefaultReplyTimeout() == null && StringUtils.hasText(defaultReplyTimeout),
						defaultReplyTimeout,
						value -> setDefaultReplyTimeout(Long.parseLong(value)));

		populateAsyncExecutorIfAny();

		boolean proxyDefaultMethods = this.gatewayAttributes.getBoolean("proxyDefaultMethods");
		if (proxyDefaultMethods) {
			setProxyDefaultMethods(proxyDefaultMethods);
		}
		super.onInit();
	}

	private void populateGatewayMethodMetadataIfAny() {
		if (getGlobalMethodMetadata() != null) {
			return;
		}

		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) getBeanFactory();

		String defaultPayloadExpression = resolveAttribute("defaultPayloadExpression");

		@SuppressWarnings("unchecked")
		Map<String, Object>[] defaultHeaders = (Map<String, Object>[]) this.gatewayAttributes.get("defaultHeaders");

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
									header -> beanFactory.resolveEmbeddedValue((String) header.get("name")),
									header -> {
										String headerValue =
												beanFactory.resolveEmbeddedValue((String) header.get("value"));
										boolean hasValue = StringUtils.hasText(headerValue);

										String headerExpression =
												beanFactory.resolveEmbeddedValue((String) header.get("expression"));

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
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) getBeanFactory();
		return beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString(attributeName));
	}

}
