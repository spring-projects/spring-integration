/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.config.annotation;

import java.io.IOException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;

/**
 * A {@link ConfigurationClassPostProcessor} implementation to register integration annotated
 * components and remove their {@link BeanDefinition}s after registration to avoid double registration
 * in case of if {@code <context:component-scan/>} is presented in the application context.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class IntegrationConfigurationClassPostProcessor extends ConfigurationClassPostProcessor {

	private volatile MetadataReaderFactory metadataReaderFactory;

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		super.setBeanClassLoader(beanClassLoader);
		this.metadataReaderFactory = new CachingMetadataReaderFactory(beanClassLoader);
	}

	@Override
	public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {
		super.processConfigBeanDefinitions(registry);
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDef = registry.getBeanDefinition(beanName);
			if (this.checkIntegrationConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {
				registry.removeBeanDefinition(beanName);
			}
		}
	}

	private boolean checkIntegrationConfigurationClassCandidate(BeanDefinition beanDef, MetadataReaderFactory metadataReaderFactory) {

		AnnotationMetadata metadata = null;
		String className = beanDef.getBeanClassName();

		if (className != null) {
			try {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(className);
				metadata = metadataReader.getAnnotationMetadata();
			}
			catch (IOException ex) {
				return false;
			}
		}

		return metadata != null &&
				metadata.getClassName().matches("org\\.springframework\\.integration\\.?\\w*\\.component.*") &&
				(metadata.isAnnotated(Configuration.class.getName()) ||
						(!metadata.isInterface() &&
								(metadata.isAnnotated(Component.class.getName()) ||
										metadata.hasAnnotatedMethods(Bean.class.getName()))));
	}

}
