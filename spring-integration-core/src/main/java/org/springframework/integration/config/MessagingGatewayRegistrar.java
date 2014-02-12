/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.beans.Introspector;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link ImportBeanDefinitionRegistrar} to parse {@link MessagingGateway} and its {@code service-interface}
 * and to register {@link BeanDefinition} {@link GatewayProxyFactoryBean}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class MessagingGatewayRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (importingClassMetadata == null || !importingClassMetadata.hasAnnotation(MessagingGateway.class.getName())) {
			return;
		}

		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(MessagingGateway.class.getName());


		BeanDefinition gatewayBeanDefinition = this.parse(importingClassMetadata);

		String id = (String) annotationAttributes.get("value");
		if (!StringUtils.hasText(id)) {
			String serviceInterface = gatewayBeanDefinition.getConstructorArgumentValues().getIndexedArgumentValue(0, Class.class).getValue().toString();
			id = Introspector.decapitalize(serviceInterface.substring(serviceInterface.lastIndexOf(".") + 1));
		}

		registry.registerBeanDefinition(id, gatewayBeanDefinition);
	}

	public BeanDefinition parse(AnnotationMetadata importingClassMetadata) {
		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(MessagingGateway.class.getName());

		String defaultPayloadExpression = (String) annotationAttributes.get("defaultPayloadExpression");

		AnnotationAttributes[] defaultHeaders = (AnnotationAttributes[]) annotationAttributes.get("defaultHeaders");
		String defaultRequestChannel = (String) annotationAttributes.get("defaultRequestChannel");
		String defaultReplyChannel = (String) annotationAttributes.get("defaultReplyChannel");
		String errorChannel = (String) annotationAttributes.get("errorChannel");
		String asyncExecutor = (String) annotationAttributes.get("asyncExecutor");
		String mapper = (String) annotationAttributes.get("mapper");

		boolean hasMapper = StringUtils.hasText(mapper);
		boolean hasDefaultPayloadExpression = StringUtils.hasText(defaultPayloadExpression);
		Assert.state(!hasMapper || !hasDefaultPayloadExpression, "'defaultPayloadExpression' is not allowed when a 'mapper' is provided");

		boolean hasDefaultHeaders = !ObjectUtils.isEmpty(defaultHeaders);
		Assert.state(!hasMapper || !hasDefaultHeaders, "'defaultHeaders' are not allowed when a 'mapper' is provided");

		BeanDefinitionBuilder gatewayProxyBuilder = BeanDefinitionBuilder.genericBeanDefinition(GatewayProxyFactoryBean.class);


		if (hasDefaultHeaders || hasDefaultPayloadExpression) {
			BeanDefinitionBuilder methodMetadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(GatewayMethodMetadata.class);
			if (hasDefaultPayloadExpression) {
				methodMetadataBuilder.addPropertyValue("payloadExpression", defaultPayloadExpression);
			}
			this.setMethodInvocationHeaders(methodMetadataBuilder, defaultHeaders);
			gatewayProxyBuilder.addPropertyValue("globalMethodMetadata", methodMetadataBuilder.getBeanDefinition());
		}


		if (StringUtils.hasText(defaultRequestChannel)) {
			gatewayProxyBuilder.addPropertyReference("defaultRequestChannel", defaultRequestChannel);
		}
		if (StringUtils.hasText(defaultReplyChannel)) {
			gatewayProxyBuilder.addPropertyReference("defaultReplyChannel", defaultReplyChannel);
		}
		if (StringUtils.hasText(errorChannel)) {
			gatewayProxyBuilder.addPropertyReference("errorChannel", errorChannel);
		}
		if (StringUtils.hasText(asyncExecutor)) {
			gatewayProxyBuilder.addPropertyReference("asyncExecutor", asyncExecutor);
		}
		if (StringUtils.hasText(mapper)) {
			gatewayProxyBuilder.addPropertyReference("mapper", mapper);
		}

		gatewayProxyBuilder.addPropertyValue("defaultRequestTimeout", annotationAttributes.get("defaultRequestTimeout"));
		gatewayProxyBuilder.addPropertyValue("defaultReplyTimeout", annotationAttributes.get("defaultReplyTimeout"));
		gatewayProxyBuilder.addPropertyValue("methodMetadataMap", annotationAttributes.get("methods"));

		String serviceInterface = importingClassMetadata.getClassName();
		if (!StringUtils.hasText(serviceInterface)) {
			serviceInterface = "org.springframework.integration.gateway.RequestReplyExchanger";
		}

		gatewayProxyBuilder.addConstructorArgValue(serviceInterface);

		return gatewayProxyBuilder.getBeanDefinition();
	}

	private void setMethodInvocationHeaders(BeanDefinitionBuilder methodMetadataBuilder, Map<String, Object>[] headerAnnotations) {
		Map<String, Object> headerExpressions = new ManagedMap<String, Object>();
		for (Map<String, Object> headerAnnotation : headerAnnotations) {
			String headerValue = (String) headerAnnotation.get("value");
			String headerExpression = (String) headerAnnotation.get("expression");
			boolean hasValue = StringUtils.hasText(headerValue);

			if (!(hasValue ^ StringUtils.hasText(headerExpression))) {
				throw new BeanDefinitionStoreException("exactly one of 'value' or 'expression' is required on a gateway's header.");
			}

			BeanDefinition expressionDef = new RootBeanDefinition(hasValue ? LiteralExpression.class : ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(hasValue ? headerValue : headerExpression);

			headerExpressions.put((String) headerAnnotation.get("name"), expressionDef);
		}
		methodMetadataBuilder.addPropertyValue("headerExpressions", headerExpressions);
	}

}
