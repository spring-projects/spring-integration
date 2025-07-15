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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.Aware;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AspectJTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} implementation to scan and register Integration specific components.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class IntegrationComponentScanRegistrar implements ImportBeanDefinitionRegistrar,
		ResourceLoaderAware, EnvironmentAware {

	private static final Log LOGGER = LogFactory.getLog(IntegrationComponentScanRegistrar.class);

	private static final String BEAN_NAME = IntegrationComponentScanRegistrar.class.getName();

	private final List<TypeFilter> defaultFilters = new ArrayList<>();

	@SuppressWarnings("NullAway.Init")
	private ResourceLoader resourceLoader;

	@SuppressWarnings("NullAway.Init")
	private Environment environment;

	public IntegrationComponentScanRegistrar() {
		this.defaultFilters.add(new AnnotationTypeFilter(MessagingGateway.class, true));
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		AnnotationAttributes componentScan =
				AnnotationAttributes.fromMap(
						importingClassMetadata.getAnnotationAttributes(IntegrationComponentScan.class.getName()));

		Assert.notNull(componentScan, "The '@IntegrationComponentScan' must be present for using this registrar");

		if (registry.containsBeanDefinition(BEAN_NAME)) {
			LOGGER.warn("Only one '@IntegrationComponentScan' can be present.\nConsider to merge them all to one.");
			return;
		}

		registry.registerBeanDefinition(BEAN_NAME,
				BeanDefinitionBuilder.genericBeanDefinition(IntegrationComponentScanRegistrar.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
						.getBeanDefinition());

		Collection<String> basePackages = getBasePackages(componentScan, registry);

		if (basePackages.isEmpty()) {
			basePackages = Collections.singleton(ClassUtils.getPackageName(importingClassMetadata.getClassName()));
		}

		ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false) {

			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				return beanDefinition.getMetadata().isIndependent()
						&& !beanDefinition.getMetadata().isAnnotation();
			}

		};

		filter(registry, componentScan, scanner); // NOSONAR - never null

		scanner.setResourceLoader(this.resourceLoader);
		scanner.setEnvironment(this.environment);

		BeanNameGenerator beanNameGenerator = IntegrationConfigUtils.annotationBeanNameGenerator(registry);

		Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");
		if (!(BeanNameGenerator.class == generatorClass)) {
			beanNameGenerator = BeanUtils.instantiateClass(generatorClass);
		}

		scanner.setBeanNameGenerator(beanNameGenerator);
		scanner.scan(basePackages.toArray(String[]::new));
	}

	private void filter(BeanDefinitionRegistry registry, AnnotationAttributes componentScan,
			ClassPathScanningCandidateComponentProvider scanner) {

		if (componentScan.getBoolean("useDefaultFilters")) { // NOSONAR - never null
			for (TypeFilter typeFilter : this.defaultFilters) {
				scanner.addIncludeFilter(typeFilter);
			}
		}

		for (AnnotationAttributes filter : componentScan.getAnnotationArray("includeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter, registry)) {
				scanner.addIncludeFilter(typeFilter);
			}
		}
		for (AnnotationAttributes filter : componentScan.getAnnotationArray("excludeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filter, registry)) {
				scanner.addExcludeFilter(typeFilter);
			}
		}
	}

	protected Collection<String> getBasePackages(AnnotationAttributes componentScan,
			@SuppressWarnings("unused") BeanDefinitionRegistry registry) {

		Set<String> basePackages = new HashSet<>();

		for (String pkg : componentScan.getStringArray("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}

		for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}

		return basePackages;
	}

	private List<TypeFilter> typeFiltersFor(AnnotationAttributes filter, BeanDefinitionRegistry registry) {
		List<TypeFilter> typeFilters = new ArrayList<>();
		FilterType filterType = filter.getEnum("type");

		for (Class<?> filterClass : filter.getClassArray("classes")) {
			switch (filterType) {
				case ANNOTATION -> {
					Assert.isAssignable(Annotation.class, filterClass,
							"An error occurred while processing a @IntegrationComponentScan ANNOTATION type filter: ");
					@SuppressWarnings("unchecked")
					Class<Annotation> annotationType = (Class<Annotation>) filterClass;
					typeFilters.add(new AnnotationTypeFilter(annotationType));
				}
				case ASSIGNABLE_TYPE -> typeFilters.add(new AssignableTypeFilter(filterClass));
				case CUSTOM -> {
					Assert.isAssignable(TypeFilter.class, filterClass,
							"An error occurred while processing a @IntegrationComponentScan CUSTOM type filter: ");
					TypeFilter typeFilter = BeanUtils.instantiateClass(filterClass, TypeFilter.class);
					invokeAwareMethods(filter, this.environment, this.resourceLoader, registry);
					typeFilters.add(typeFilter);
				}
				default -> throw new IllegalArgumentException("Filter type not supported with Class value: " +
						filterType);
			}
		}

		for (String expression : filter.getStringArray("pattern")) {
			switch (filterType) {
				case ASPECTJ ->
						typeFilters.add(new AspectJTypeFilter(expression, this.resourceLoader.getClassLoader()));
				case REGEX -> typeFilters.add(new RegexPatternTypeFilter(Pattern.compile(expression)));
				default -> throw new IllegalArgumentException("Filter type not supported with String pattern: "
						+ filterType);
			}
		}

		return typeFilters;
	}

	private static void invokeAwareMethods(Object parserStrategyBean, Environment environment,
			ResourceLoader resourceLoader, BeanDefinitionRegistry registry) {

		if (parserStrategyBean instanceof Aware) {
			if (parserStrategyBean instanceof BeanClassLoaderAware) {
				ClassLoader classLoader =
						registry instanceof ConfigurableBeanFactory
								? ((ConfigurableBeanFactory) registry).getBeanClassLoader()
								: resourceLoader.getClassLoader();
				if (classLoader != null) {
					((BeanClassLoaderAware) parserStrategyBean).setBeanClassLoader(classLoader);
				}
			}
			if (parserStrategyBean instanceof BeanFactoryAware && registry instanceof BeanFactory) {
				((BeanFactoryAware) parserStrategyBean).setBeanFactory((BeanFactory) registry);
			}
			if (parserStrategyBean instanceof EnvironmentAware) {
				((EnvironmentAware) parserStrategyBean).setEnvironment(environment);
			}
			if (parserStrategyBean instanceof ResourceLoaderAware) {
				((ResourceLoaderAware) parserStrategyBean).setResourceLoader(resourceLoader);
			}
		}
	}

}
