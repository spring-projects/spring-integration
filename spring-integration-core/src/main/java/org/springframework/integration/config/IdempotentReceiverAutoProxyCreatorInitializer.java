/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.annotation.IdempotentReceiver;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * The {@link IntegrationConfigurationInitializer} that populates
 * the {@link ConfigurableListableBeanFactory}
 * with an {@link IdempotentReceiverAutoProxyCreator}
 * when {@code IdempotentReceiverInterceptor} {@link BeanDefinition}s and their {@code mapping}
 * to Consumer Endpoints are present.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 4.1
 */
public class IdempotentReceiverAutoProxyCreatorInitializer implements IntegrationConfigurationInitializer {

	public static final String IDEMPOTENT_ENDPOINTS_MAPPING = "IDEMPOTENT_ENDPOINTS_MAPPING";

	private static final String IDEMPOTENT_RECEIVER_AUTO_PROXY_CREATOR_BEAN_NAME =
			IdempotentReceiverAutoProxyCreator.class.getName();

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		List<Map<String, String>> idempotentEndpointsMapping = new ManagedList<>();

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (IdempotentReceiverInterceptor.class.getName().equals(beanDefinition.getBeanClassName())) {
				Object value = beanDefinition.removeAttribute(IDEMPOTENT_ENDPOINTS_MAPPING);
				Assert.isInstanceOf(String.class, value,
						"The 'mapping' of BeanDefinition 'IDEMPOTENT_ENDPOINTS_MAPPING' must be String.");
				String mapping = (String) value;
				String[] endpoints = StringUtils.tokenizeToStringArray(mapping, ",");
				for (String endpoint : endpoints) {
					Map<String, String> idempotentEndpoint = new ManagedMap<>();
					idempotentEndpoint.put(beanName,
							beanFactory.resolveEmbeddedValue(endpoint) + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX);
					idempotentEndpointsMapping.add(idempotentEndpoint);
				}
			}
			else if (beanDefinition instanceof AnnotatedBeanDefinition) {
				annotated(beanFactory, idempotentEndpointsMapping, beanName, beanDefinition);
			}
		}

		if (!idempotentEndpointsMapping.isEmpty()) {
			BeanDefinition bd =
					BeanDefinitionBuilder.rootBeanDefinition(IdempotentReceiverAutoProxyCreator.class)
							.addPropertyValue("idempotentEndpointsMapping", idempotentEndpointsMapping)
							.getBeanDefinition();
			registry.registerBeanDefinition(IDEMPOTENT_RECEIVER_AUTO_PROXY_CREATOR_BEAN_NAME, bd);
		}
	}

	private void annotated(ConfigurableListableBeanFactory beanFactory,
			List<Map<String, String>> idempotentEndpointsMapping, String beanName, BeanDefinition beanDefinition)
			throws LinkageError {

		if (beanDefinition.getSource() instanceof MethodMetadata beanMethod) {
			String annotationType = IdempotentReceiver.class.getName();
			if (beanMethod.isAnnotated(annotationType)) { // NOSONAR never null
				Object value = Objects.requireNonNull(beanMethod.getAnnotationAttributes(annotationType)).get("value");
				if (value != null) {

					Class<?> returnType;
					if (beanMethod instanceof StandardMethodMetadata) {
						returnType = ((StandardMethodMetadata) beanMethod).getIntrospectedMethod()
								.getReturnType();
					}
					else {
						try {
							returnType = ClassUtils.forName(beanMethod.getReturnTypeName(),
									beanFactory.getBeanClassLoader());
						}
						catch (ClassNotFoundException e) {
							throw new CannotLoadBeanClassException(beanDefinition.getDescription(),
									beanName, beanMethod.getReturnTypeName(), e);
						}
					}

					String endpoint = beanName;
					if (!MessageHandler.class.isAssignableFrom(returnType)) {
						endpoint = beanName + ".*" + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
					}

					String[] interceptors = (String[]) value;
					for (String interceptor : interceptors) {
						Map<String, String> idempotentEndpoint = new ManagedMap<>();
						idempotentEndpoint.put(interceptor, endpoint);
						idempotentEndpointsMapping.add(idempotentEndpoint);
					}
				}
			}
		}
	}

}
