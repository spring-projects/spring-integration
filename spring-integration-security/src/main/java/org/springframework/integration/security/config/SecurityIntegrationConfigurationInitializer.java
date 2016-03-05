/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.security.config;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.security.channel.ChannelAccessPolicy;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.integration.security.channel.DefaultChannelAccessPolicy;
import org.springframework.integration.security.channel.SecuredChannel;

/**
 * The Integration Security infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class SecurityIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String CHANNEL_SECURITY_INTERCEPTOR_BPP_BEAN_NAME =
			ChannelSecurityInterceptorBeanPostProcessor.class.getName();

	@Override
	@SuppressWarnings("unchecked")
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		Map<String, ManagedSet<String>> securityInterceptors = new ManagedMap<String, ManagedSet<String>>();
		Map<String, Map<Pattern, ChannelAccessPolicy>> policies = new HashMap<String, Map<Pattern, ChannelAccessPolicy>>();

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (ChannelSecurityInterceptor.class.getName().equals(beanDefinition.getBeanClassName())) {
				BeanDefinition metadataSource = (BeanDefinition) beanDefinition.getConstructorArgumentValues()
						.getIndexedArgumentValue(0, BeanDefinition.class)
						.getValue();

				Map<String, ?> value = (Map<String, ?>) metadataSource.getConstructorArgumentValues()
						.getIndexedArgumentValue(0, Map.class)
						.getValue();
				ManagedSet<String> patterns = new ManagedSet<String>();
				if (!securityInterceptors.containsKey(beanName)) {
					securityInterceptors.put(beanName, patterns);
				}
				else {
					patterns = securityInterceptors.get(beanName);
				}
				patterns.addAll(value.keySet());
			}
			else if (beanDefinition instanceof AnnotatedBeanDefinition) {
				if (beanDefinition.getSource() instanceof MethodMetadata) {
					MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
					String annotationType = SecuredChannel.class.getName();
					if (beanMethod.isAnnotated(annotationType)) {
						Map<String, Object> securedAttributes = beanMethod.getAnnotationAttributes(annotationType);
						String[] interceptors = (String[]) securedAttributes.get("interceptor");
						String[] sendAccess = (String[]) securedAttributes.get("sendAccess");
						String[] receiveAccess = (String[]) securedAttributes.get("receiveAccess");
						ChannelAccessPolicy accessPolicy = new DefaultChannelAccessPolicy(sendAccess, receiveAccess);
						for (String interceptor : interceptors) {
							ManagedSet<String> patterns = new ManagedSet<String>();
							if (!securityInterceptors.containsKey(interceptor)) {
								securityInterceptors.put(interceptor, patterns);
							}
							else {
								patterns = securityInterceptors.get(interceptor);
							}
							patterns.add(beanName);

							Map<Pattern, ChannelAccessPolicy> mapping = new HashMap<Pattern, ChannelAccessPolicy>();
							if (!policies.containsKey(interceptor)) {
								policies.put(interceptor, mapping);
							}
							else {
								mapping = policies.get(interceptor);
							}
							mapping.put(Pattern.compile(beanName), accessPolicy);
						}
					}
				}
			}
		}

		if (!securityInterceptors.isEmpty()) {

			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.rootBeanDefinition(ChannelSecurityInterceptorBeanPostProcessor.class)
					.addConstructorArgValue(securityInterceptors);
			if (!policies.isEmpty()) {
				builder.addConstructorArgValue(policies);
			}
			registry.registerBeanDefinition(CHANNEL_SECURITY_INTERCEPTOR_BPP_BEAN_NAME, builder.getBeanDefinition());
		}
	}

}
