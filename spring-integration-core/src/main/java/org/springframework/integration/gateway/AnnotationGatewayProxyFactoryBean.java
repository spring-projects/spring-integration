/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.gateway;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.AnnotationConstants;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link GatewayProxyFactoryBean} extension for Java configuration.
 * The service interface may be marked with the {@link MessagingGateway} annotation.
 * Otherwise the default state is applied.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class AnnotationGatewayProxyFactoryBean extends GatewayProxyFactoryBean {

	private final AnnotationAttributes gatewayAttributes;

	public AnnotationGatewayProxyFactoryBean(Class<?> serviceInterface) {
		super(serviceInterface);
		AnnotationAttributes gatewayAttributes =
				AnnotatedElementUtils.getMergedAnnotationAttributes(serviceInterface,
						MessagingGateway.class.getName(), false, true);
		if (gatewayAttributes == null) {
			gatewayAttributes = AnnotationUtils.getAnnotationAttributes(
					AnnotationUtils.synthesizeAnnotation(MessagingGateway.class), false, true);
		}

		this.gatewayAttributes = gatewayAttributes;

		String id = gatewayAttributes.getString("name");
		if (!StringUtils.hasText(id)) {
			setBeanName(id);
		}
	}

	@Override
	protected void onInit() {
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) getBeanFactory();

		String defaultPayloadExpression =
				beanFactory.resolveEmbeddedValue(
						this.gatewayAttributes.getString("defaultPayloadExpression"));

		@SuppressWarnings("unchecked")
		Map<String, Object>[] defaultHeaders = (Map<String, Object>[]) this.gatewayAttributes.get("defaultHeaders");

		String mapper = beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("mapper"));

		boolean hasMapper = StringUtils.hasText(mapper);
		boolean hasDefaultPayloadExpression = StringUtils.hasText(defaultPayloadExpression);
		Assert.state(!hasMapper || !hasDefaultPayloadExpression,
				"'defaultPayloadExpression' is not allowed when a 'mapper' is provided");

		boolean hasDefaultHeaders = !ObjectUtils.isEmpty(defaultHeaders);
		Assert.state(!hasMapper || !hasDefaultHeaders,
				"'defaultHeaders' are not allowed when a 'mapper' is provided");


		String defaultRequestChannel =
				beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("defaultRequestChannel"));
		setDefaultRequestChannelName(defaultRequestChannel);

		String defaultReplyChannel =
				beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("defaultReplyChannel"));
		setDefaultReplyChannelName(defaultReplyChannel);

		String errorChannel = beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("errorChannel"));
		setErrorChannelName(errorChannel);

		String asyncExecutor = beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("asyncExecutor"));
		if (asyncExecutor == null || AnnotationConstants.NULL.equals(asyncExecutor)) {
			setAsyncExecutor(null);
		}
		else if (StringUtils.hasText(asyncExecutor)) {
			setAsyncExecutor(beanFactory.getBean(asyncExecutor, Executor.class));
		}

		if (hasDefaultHeaders || hasDefaultPayloadExpression) {
			GatewayMethodMetadata gatewayMethodMetadata = new GatewayMethodMetadata();

			if (hasDefaultPayloadExpression) {
				gatewayMethodMetadata.setPayloadExpression(defaultPayloadExpression);
			}

			Map<String, Expression> headerExpressions = new HashMap<>();
			for (Map<String, Object> header : defaultHeaders) {
				String headerValue = beanFactory.resolveEmbeddedValue((String) header.get("value"));
				boolean hasValue = StringUtils.hasText(headerValue);

				String headerExpression = beanFactory.resolveEmbeddedValue((String) header.get("expression"));

				Assert.state(!(hasValue == StringUtils.hasText(headerExpression)),
						"exactly one of 'value' or 'expression' is required on a gateway's header.");

				Expression expression = hasValue ?
						new LiteralExpression(headerValue) :
						EXPRESSION_PARSER.parseExpression(headerExpression);

				String headerName = beanFactory.resolveEmbeddedValue((String) header.get("name"));
				headerExpressions.put(headerName, expression);
			}

			gatewayMethodMetadata.setHeaderExpressions(headerExpressions);

			setGlobalMethodMetadata(gatewayMethodMetadata);
		}

		if (StringUtils.hasText(mapper)) {
			setMapper(beanFactory.getBean(mapper, MethodArgsMessageMapper.class));
		}

		String defaultRequestTimeout =
				beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("defaultRequestTimeout"));
		setDefaultRequestTimeout(Long.parseLong(defaultRequestTimeout));

		String defaultReplyTimeout =
				beanFactory.resolveEmbeddedValue(this.gatewayAttributes.getString("defaultReplyTimeout"));
		setDefaultReplyTimeout(Long.parseLong(defaultReplyTimeout));

		super.onInit();
	}

}
