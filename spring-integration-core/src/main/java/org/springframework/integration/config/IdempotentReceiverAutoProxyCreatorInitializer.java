/*
 * Copyright 2014-2015 the original author or authors.
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

package org.springframework.integration.config;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
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
import org.springframework.util.StringUtils;

/**
 * The {@link IntegrationConfigurationInitializer} that populates
 * the {@link ConfigurableListableBeanFactory}
 * with an {@link IdempotentReceiverAutoProxyCreator}
 * when {@code IdempotentReceiverInterceptor} {@link BeanDefinition}s and their {@code mapping}
 * to Consumer Endpoints are present.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentReceiverAutoProxyCreatorInitializer implements IntegrationConfigurationInitializer {

	public static final String IDEMPOTENT_ENDPOINTS_MAPPING = "IDEMPOTENT_ENDPOINTS_MAPPING";

	private static final String IDEMPOTENT_RECEIVER_AUTO_PROXY_CREATOR_BEAN_NAME =
			IdempotentReceiverAutoProxyCreator.class.getName();

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		List<Map<String, String>> idempotentEndpointsMapping = new ManagedList<Map<String, String>>();

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (IdempotentReceiverInterceptor.class.getName().equals(beanDefinition.getBeanClassName())) {
				Object value = beanDefinition.removeAttribute(IDEMPOTENT_ENDPOINTS_MAPPING);
				Assert.isInstanceOf(String.class, value,
						"The 'mapping' of BeanDefinition 'IDEMPOTENT_ENDPOINTS_MAPPING' must be String.");
				String mapping = (String) value;
				String[] endpoints = StringUtils.tokenizeToStringArray(mapping, ",");
				for (String endpoint : endpoints) {
					Map<String, String> idempotentEndpoint = new ManagedMap<String, String>();
					idempotentEndpoint.put(beanName,
							beanFactory.resolveEmbeddedValue(endpoint) + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX);
					idempotentEndpointsMapping.add(idempotentEndpoint);
				}
			}
			else if (beanDefinition instanceof AnnotatedBeanDefinition) {
				if (beanDefinition.getSource() instanceof MethodMetadata) {
					MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
					String annotationType = IdempotentReceiver.class.getName();
					if (beanMethod.isAnnotated(annotationType)) {
						Object value = beanMethod.getAnnotationAttributes(annotationType).get("value");
						if (value != null) {
							String[] interceptors = (String[]) value;
							String endpoint = beanName;
							if (!MessageHandler.class.isAssignableFrom(
									((StandardMethodMetadata) beanMethod).getIntrospectedMethod().getReturnType())) {
								/*
								   MessageHandler beans, populated from @Bean methods, have a complex id,
								   including @Configuration bean name, method name and the Messaging annotation name.
								   The following pattern matches the bean name, regardless of the annotation name.
								*/
								endpoint = beanDefinition.getFactoryBeanName() + "." + beanName +
										".*" + IntegrationConfigUtils.HANDLER_ALIAS_SUFFIX;
							}
							for (String interceptor : interceptors) {
								Map<String, String> idempotentEndpoint = new ManagedMap<String, String>();
								idempotentEndpoint.put(interceptor, endpoint);
								idempotentEndpointsMapping.add(idempotentEndpoint);
							}
						}
					}
				}
			}
		}

		if (!idempotentEndpointsMapping.isEmpty()) {
			BeanDefinition bd = BeanDefinitionBuilder.rootBeanDefinition(IdempotentReceiverAutoProxyCreator.class)
					.addPropertyValue("idempotentEndpointsMapping", idempotentEndpointsMapping)
					.getBeanDefinition();
			registry.registerBeanDefinition(IDEMPOTENT_RECEIVER_AUTO_PROXY_CREATOR_BEAN_NAME, bd);
		}
	}

}
