/*
 * Copyright 2018-2024 the original author or authors.
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

package org.springframework.integration.dsl.lifecycle;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringValueResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 5.0.7
 */
@SpringJUnitConfig
@DirtiesContext
public class IntegrationComponentSpecLifecycleTests {

	@Autowired
	private MyComponent myComponent;

	@Test
	public void testIntegrationComponentSpecLifecycle() {
		assertThat(this.myComponent.initialized).isTrue();
		assertThat(this.myComponent.name).isEqualTo("testSpec");
		assertThat(this.myComponent.applicationContext).isNotNull();
		assertThat(this.myComponent.beanFactory).isNotNull();
		assertThat(this.myComponent.applicationEventPublisher).isNotNull();
		assertThat(this.myComponent.classLoader).isNotNull();
		assertThat(this.myComponent.environment).isNotNull();
		assertThat(this.myComponent.messageSource).isNotNull();
		assertThat(this.myComponent.resolver).isNotNull();
		assertThat(this.myComponent.resourceLoader).isNotNull();
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public IntegrationComponentSpec<?, ?> testSpec() {
			return new MyIntegrationComponentSpec();
		}

	}

	private static final class MyIntegrationComponentSpec
			extends IntegrationComponentSpec<MyIntegrationComponentSpec, MyComponent> {

		MyIntegrationComponentSpec() {
			this.target = new MyComponent();
		}

	}

	private static final class MyComponent
			implements InitializingBean, BeanNameAware, BeanFactoryAware, ApplicationContextAware,
			BeanClassLoaderAware, EnvironmentAware, EmbeddedValueResolverAware, ResourceLoaderAware,
			ApplicationEventPublisherAware, MessageSourceAware {

		private boolean initialized;

		private ClassLoader classLoader;

		private BeanFactory beanFactory;

		private String name;

		private ApplicationContext applicationContext;

		private ApplicationEventPublisher applicationEventPublisher;

		private StringValueResolver resolver;

		private Environment environment;

		private MessageSource messageSource;

		private ResourceLoader resourceLoader;

		@Override
		public void setBeanClassLoader(ClassLoader classLoader) {
			this.classLoader = classLoader;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		public void setBeanName(String name) {
			this.name = name;
		}

		@Override
		public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
			this.applicationContext = applicationContext;
		}

		@Override
		public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
			this.applicationEventPublisher = applicationEventPublisher;
		}

		@Override
		public void setEmbeddedValueResolver(StringValueResolver resolver) {
			this.resolver = resolver;
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void setMessageSource(MessageSource messageSource) {
			this.messageSource = messageSource;
		}

		@Override
		public void setResourceLoader(ResourceLoader resourceLoader) {
			this.resourceLoader = resourceLoader;
		}

		@Override
		public void afterPropertiesSet() {
			this.initialized = true;
		}

	}

}
