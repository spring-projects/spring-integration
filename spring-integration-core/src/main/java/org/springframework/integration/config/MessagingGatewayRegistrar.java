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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.config.annotation.MessagingAnnotationUtils;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.integration.gateway.GatewayProxyFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link ImportBeanDefinitionRegistrar} to parse {@link MessagingGateway} and its {@code service-interface}
 * and to register {@link BeanDefinition} {@link GatewayProxyFactoryBean}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
public class MessagingGatewayRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log logger = LogFactory.getLog(MessagingGatewayRegistrar.class);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (importingClassMetadata != null && importingClassMetadata.isAnnotated(MessagingGateway.class.getName())) {
			Assert.isTrue(importingClassMetadata.isInterface(),	"@MessagingGateway can only be specified on an interface");
			List<MultiValueMap<String, Object>> valuesHierarchy = captureMetaAnnotationValues(importingClassMetadata);
			Map<String, Object> annotationAttributes =
					importingClassMetadata.getAnnotationAttributes(MessagingGateway.class.getName());
			replaceEmptyOverrides(valuesHierarchy, annotationAttributes);
			annotationAttributes.put("serviceInterface", importingClassMetadata.getClassName());

			BeanDefinitionReaderUtils.registerBeanDefinition(this.parse(annotationAttributes), registry);
		}
	}

	public BeanDefinitionHolder parse(Map<String, Object> gatewayAttributes) {

		String defaultPayloadExpression = (String) gatewayAttributes.get("defaultPayloadExpression");

		@SuppressWarnings("unchecked")
		Map<String, Object>[] defaultHeaders = (Map<String, Object>[]) gatewayAttributes.get("defaultHeaders");

		String defaultRequestChannel = (String) gatewayAttributes.get("defaultRequestChannel");
		String defaultReplyChannel = (String) gatewayAttributes.get("defaultReplyChannel");
		String errorChannel = (String) gatewayAttributes.get("errorChannel");
		String asyncExecutor = (String) gatewayAttributes.get("asyncExecutor");
		String mapper = (String) gatewayAttributes.get("mapper");

		boolean hasMapper = StringUtils.hasText(mapper);
		boolean hasDefaultPayloadExpression = StringUtils.hasText(defaultPayloadExpression);
		Assert.state(!hasMapper || !hasDefaultPayloadExpression,
				"'defaultPayloadExpression' is not allowed when a 'mapper' is provided");

		boolean hasDefaultHeaders = !ObjectUtils.isEmpty(defaultHeaders);
		Assert.state(!hasMapper || !hasDefaultHeaders, "'defaultHeaders' are not allowed when a 'mapper' is provided");

		BeanDefinitionBuilder gatewayProxyBuilder = BeanDefinitionBuilder.genericBeanDefinition(GatewayProxyFactoryBean.class);

		if (hasDefaultHeaders || hasDefaultPayloadExpression) {
			BeanDefinitionBuilder methodMetadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(GatewayMethodMetadata.class);
			if (hasDefaultPayloadExpression) {
				methodMetadataBuilder.addPropertyValue("payloadExpression", defaultPayloadExpression);
			}
			Map<String, Object> headerExpressions = new ManagedMap<String, Object>();
			for (Map<String, Object> header : defaultHeaders) {
				String headerValue = (String) header.get("value");
				String headerExpression = (String) header.get("expression");
				boolean hasValue = StringUtils.hasText(headerValue);

				if (!(hasValue ^ StringUtils.hasText(headerExpression))) {
					throw new BeanDefinitionStoreException("exactly one of 'value' or 'expression' " +
							"is required on a gateway's header.");
				}

				BeanDefinition expressionDef =
						new RootBeanDefinition(hasValue ? LiteralExpression.class : ExpressionFactoryBean.class);
				expressionDef.getConstructorArgumentValues()
						.addGenericArgumentValue(hasValue ? headerValue : headerExpression);

				headerExpressions.put((String) header.get("name"), expressionDef);
			}
			methodMetadataBuilder.addPropertyValue("headerExpressions", headerExpressions);
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

		gatewayProxyBuilder.addPropertyValue("defaultRequestTimeout", gatewayAttributes.get("defaultRequestTimeout"));
		gatewayProxyBuilder.addPropertyValue("defaultReplyTimeout", gatewayAttributes.get("defaultReplyTimeout"));
		gatewayProxyBuilder.addPropertyValue("methodMetadataMap", gatewayAttributes.get("methods"));


		String serviceInterface = (String) gatewayAttributes.get("serviceInterface");
		if (!StringUtils.hasText(serviceInterface)) {
			serviceInterface = "org.springframework.integration.gateway.RequestReplyExchanger";
		}
		String id = (String) gatewayAttributes.get("name");
		if (!StringUtils.hasText(id)) {
			id = Introspector.decapitalize(serviceInterface.substring(serviceInterface.lastIndexOf(".") + 1));
		}

		gatewayProxyBuilder.addConstructorArgValue(serviceInterface);

		return new BeanDefinitionHolder(gatewayProxyBuilder.getBeanDefinition(), id);
	}

	/**
	 * TODO until SPR-11710 will be resolved.
	 * Captures the meta-annotation attribute values, in order.
	 * @param importingClassMetadata The importing class metadata
	 * @return The captured values.
	 */
	private List<MultiValueMap<String, Object>> captureMetaAnnotationValues(AnnotationMetadata importingClassMetadata) {
		Set<String> directAnnotations = importingClassMetadata.getAnnotationTypes();
		List<MultiValueMap<String, Object>> valuesHierarchy = new ArrayList<MultiValueMap<String, Object>>();
		// Need to grab the values now; see SPR-11710
		for (String ann : directAnnotations) {
			Set<String> chain = importingClassMetadata.getMetaAnnotationTypes(ann);
			if (chain.contains(MessagingGateway.class.getName())) {
				for (String meta : chain) {
					valuesHierarchy.add(importingClassMetadata.getAllAnnotationAttributes(meta));
				}
			}
		}
		return valuesHierarchy;
	}

	/**
	 * TODO until SPR-11709 will be resolved.
	 * For any empty values, traverses back up the meta-annotation hierarchy to
	 * see if a value has been overridden to empty, and replaces the first such value found.
	 * @param valuesHierarchy The values hierarchy in order.
	 * @param annotationAttributes The current attribute values.
	 */
	private void replaceEmptyOverrides(List<MultiValueMap<String, Object>> valuesHierarchy,
			Map<String, Object> annotationAttributes) {
		for (Entry<String, Object> entry : annotationAttributes.entrySet()) {
			Object value = entry.getValue();
			if (!MessagingAnnotationUtils.hasValue(value)) {
				// see if we overrode a value that was higher in the annotation chain
				for (MultiValueMap<String, Object> metaAttributesMap : valuesHierarchy) {
					Object newValue = metaAttributesMap.getFirst(entry.getKey());
					if (MessagingAnnotationUtils.hasValue(newValue)) {
						annotationAttributes.put(entry.getKey(), newValue);
						break;
					}
				}
			}
		}
	}

}
