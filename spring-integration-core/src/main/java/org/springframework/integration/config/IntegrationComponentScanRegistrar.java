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

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} implementation to scan and register Integration specific components.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class IntegrationComponentScanRegistrar implements ImportBeanDefinitionRegistrar,
		ResourceLoaderAware, BeanClassLoaderAware {

	private final Map<TypeFilter, ImportBeanDefinitionRegistrar> componentRegistrars = new HashMap<TypeFilter, ImportBeanDefinitionRegistrar>();

	private ResourceLoader resourceLoader;

	private ClassLoader classLoader;

	public IntegrationComponentScanRegistrar() {
		this.componentRegistrars.put(new AnnotationTypeFilter(MessagingGateway.class, true), new MessagingGatewayRegistrar());
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, Object> componentScan = importingClassMetadata
				.getAnnotationAttributes("org.springframework.integration.annotation.IntegrationComponentScan");

		Set<String> basePackages = new HashSet<String>();
		for (String pkg : (String[]) componentScan.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : (String[]) componentScan.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class[]) componentScan.get("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		if (basePackages.isEmpty()) {
			basePackages.add(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}

		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {

			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				if (beanDefinition.getMetadata().isIndependent()) {
					// TODO until SPR-11711 will be resolved
					if (beanDefinition.getMetadata().isInterface() &&
							beanDefinition.getMetadata().getInterfaceNames().length == 1 &&
							Annotation.class.getName().equals(beanDefinition.getMetadata().getInterfaceNames()[0])) {
						try {
							Class<?> target = ClassUtils.forName(beanDefinition.getMetadata().getClassName(), classLoader);
							return !target.isAnnotation();
						}
						catch (Exception e) {
							logger.error("Could not load target class: " + beanDefinition.getMetadata().getClassName(), e);

						}
					}
					return true;
				}
				return false;
			}
		};

		for (TypeFilter typeFilter : componentRegistrars.keySet()) {
			scanner.addIncludeFilter(typeFilter);
		}

		scanner.setResourceLoader(resourceLoader);

		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					for (ImportBeanDefinitionRegistrar importBeanDefinitionRegistrar : componentRegistrars.values()) {
						importBeanDefinitionRegistrar.registerBeanDefinitions(((AnnotatedBeanDefinition) candidateComponent).getMetadata(),
								registry);
					}
				}
			}
		}
	}

}
