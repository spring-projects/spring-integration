/*
 * Copyright 2014-2023 the original author or authors.
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

import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.integration.channel.DirectChannel;

/**
 * Shared utility methods for Integration configuration.
 *
 * @author Artem Bilan
 * @author Chris Bono
 *
 * @since 4.0
 */
public final class IntegrationConfigUtils {

	public static final String HANDLER_ALIAS_SUFFIX = ".handler";

	public static void registerSpelFunctionBean(BeanDefinitionRegistry registry, String functionId, String className,
			String methodSignature) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SpelFunctionFactoryBean.class)
				.addConstructorArgValue(className)
				.addConstructorArgValue(methodSignature);
		registry.registerBeanDefinition(functionId, builder.getBeanDefinition());
	}

	/**
	 * Register a {@link SpelFunctionFactoryBean} for the provided method signature.
	 * @param registry the registry for bean to register
	 * @param functionId the bean name
	 * @param aClass the class for function
	 * @param methodSignature the function method to be called from SpEL
	 * @since 5.5
	 */
	public static void registerSpelFunctionBean(BeanDefinitionRegistry registry, String functionId, Class<?> aClass,
			String methodSignature) {

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(SpelFunctionFactoryBean.class)
						.addConstructorArgValue(aClass)
						.addConstructorArgValue(methodSignature);
		registry.registerBeanDefinition(functionId, builder.getBeanDefinition());
	}

	public static void autoCreateDirectChannel(String channelName, BeanDefinitionRegistry registry) {
		registry.registerBeanDefinition(channelName, new RootBeanDefinition(DirectChannel.class));
	}

	public static BeanNameGenerator annotationBeanNameGenerator(BeanDefinitionRegistry registry) {
		BeanNameGenerator beanNameGenerator = AnnotationBeanNameGenerator.INSTANCE;
		if (registry instanceof SingletonBeanRegistry singletonBeanRegistry) {
			BeanNameGenerator generator =
					(BeanNameGenerator) singletonBeanRegistry.getSingleton(
							AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);
			if (generator != null) {
				beanNameGenerator = generator;
			}
		}

		return beanNameGenerator;
	}

	private IntegrationConfigUtils() {
	}

}
