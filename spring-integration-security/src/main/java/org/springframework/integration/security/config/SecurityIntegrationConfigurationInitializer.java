/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.security.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
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
 *
 * @since 4.0
 *
 * @deprecated since 6.0 in favor of literally
 * {@code new AuthorizationChannelInterceptor(AuthorityAuthorizationManager.hasAnyRole())}
 */
@Deprecated(since = "6.0", forRemoval = true)
public class SecurityIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String CHANNEL_SECURITY_INTERCEPTOR_BPP_BEAN_NAME =
			ChannelSecurityInterceptorBeanPostProcessor.class.getName();

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		Map<String, Set<String>> securityInterceptors = new ManagedMap<>();
		Map<String, Map<Pattern, ChannelAccessPolicy>> policies = new HashMap<>();

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (ChannelSecurityInterceptor.class.getName().equals(beanDefinition.getBeanClassName())) {
				collectPatternsFromInterceptor(securityInterceptors, beanName, beanDefinition);
			}
			else if (beanDefinition instanceof AnnotatedBeanDefinition) {
				Object beanSource = beanDefinition.getSource();
				if (beanSource instanceof MethodMetadata) {
					collectInterceptorsAndPoliciesBySecuredChannel(securityInterceptors, policies, beanName,
							(MethodMetadata) beanSource);
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

	@SuppressWarnings("unchecked")
	private void collectPatternsFromInterceptor(Map<String, Set<String>> securityInterceptors, String beanName,
			BeanDefinition beanDefinition) {

		ConstructorArgumentValues.ValueHolder metadataSourceValueHolder =
				beanDefinition
						.getConstructorArgumentValues()
						.getIndexedArgumentValue(0, BeanDefinition.class);
		if (metadataSourceValueHolder != null) {
			BeanDefinition metadataSource = (BeanDefinition) metadataSourceValueHolder.getValue();
			if (metadataSource != null) {
				ConstructorArgumentValues.ValueHolder patternMappingsValueHolder =
						metadataSource
								.getConstructorArgumentValues()
								.getIndexedArgumentValue(0, Map.class);
				if (patternMappingsValueHolder != null) {
					Map<String, ?> patternsToAdd = (Map<String, ?>) patternMappingsValueHolder.getValue();
					Set<String> patterns = new ManagedSet<>();
					if (!securityInterceptors.containsKey(beanName)) {
						securityInterceptors.put(beanName, patterns);
					}
					else {
						patterns = securityInterceptors.get(beanName);
					}
					if (patternsToAdd != null) {
						patterns.addAll(patternsToAdd.keySet());
					}
				}
			}
		}
	}

	private void collectInterceptorsAndPoliciesBySecuredChannel(Map<String, Set<String>> securityInterceptors,
			Map<String, Map<Pattern, ChannelAccessPolicy>> policies, String beanName, MethodMetadata beanMethod) {

		Map<String, Object> securedAttributes = beanMethod.getAnnotationAttributes(SecuredChannel.class.getName());
		if (securedAttributes != null) {
			String[] interceptors = (String[]) securedAttributes.get("interceptor");
			String[] sendAccess = (String[]) securedAttributes.get("sendAccess");
			String[] receiveAccess = (String[]) securedAttributes.get("receiveAccess");
			ChannelAccessPolicy accessPolicy = new DefaultChannelAccessPolicy(sendAccess, receiveAccess);
			for (String interceptor : interceptors) {
				Set<String> patterns = new ManagedSet<>();
				if (!securityInterceptors.containsKey(interceptor)) {
					securityInterceptors.put(interceptor, patterns);
				}
				else {
					patterns = securityInterceptors.get(interceptor);
				}
				patterns.add(beanName);

				Map<Pattern, ChannelAccessPolicy> mapping = new HashMap<>();
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
