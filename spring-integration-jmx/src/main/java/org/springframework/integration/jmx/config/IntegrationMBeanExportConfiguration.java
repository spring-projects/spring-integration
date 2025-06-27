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

package org.springframework.integration.jmx.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.context.expression.StandardBeanExpressionResolver;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code @Configuration} class that registers a {@link IntegrationMBeanExporter} bean.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableIntegrationMBeanExport} annotation. See its javadoc for complete usage details.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class IntegrationMBeanExportConfiguration implements ImportAware, EnvironmentAware, BeanFactoryAware {

	/**
	 * A name for default {@link IntegrationMBeanExporter} bean.
	 */
	public static final String MBEAN_EXPORTER_NAME = "integrationMbeanExporter";

	@SuppressWarnings("NullAway.Init")
	private AnnotationAttributes attributes;

	@SuppressWarnings("NullAway.Init")
	private BeanFactory beanFactory;

	@SuppressWarnings("NullAway.Init")
	private BeanExpressionResolver resolver;

	@SuppressWarnings("NullAway.Init")
	private BeanExpressionContext expressionContext;

	@SuppressWarnings("NullAway.Init")
	private Environment environment;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		Assert.isInstanceOf(ConfigurableBeanFactory.class, this.beanFactory);
		var configurableBeanFactory = (ConfigurableBeanFactory) this.beanFactory;
		var beanExpressionResolver = configurableBeanFactory.getBeanExpressionResolver();
		if (beanExpressionResolver == null) {
			beanExpressionResolver = new StandardBeanExpressionResolver(configurableBeanFactory.getBeanClassLoader());
		}
		this.resolver = beanExpressionResolver;
		this.expressionContext = new BeanExpressionContext(configurableBeanFactory, null);
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, @Nullable Object> map =
				importMetadata.getAnnotationAttributes(EnableIntegrationMBeanExport.class.getName());
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(map);
		Assert.notNull(attributes, () ->
				"@EnableIntegrationMBeanExport is not present on importing class: " + importMetadata.getClassName());
		this.attributes = attributes;
	}

	@Bean(name = MBEAN_EXPORTER_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public IntegrationMBeanExporter mbeanExporter() {
		IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
		exporter.setRegistrationPolicy(this.attributes.getEnum("registration"));
		setupDomain(exporter);
		setupServer(exporter);
		setupComponentNamePatterns(exporter);
		return exporter;
	}

	private void setupDomain(IntegrationMBeanExporter exporter) {
		String defaultDomain = this.attributes.getString("defaultDomain");
		defaultDomain = this.environment.resolvePlaceholders(defaultDomain);
		if (StringUtils.hasText(defaultDomain)) {
			exporter.setDefaultDomain(defaultDomain);
		}
	}

	private void setupServer(IntegrationMBeanExporter exporter) {
		String server = this.attributes.getString("server");
		server = this.environment.resolvePlaceholders(server);
		if (StringUtils.hasText(server)) {
			MBeanServer bean;
			if (server.startsWith("#{") && server.endsWith("}")) {
				bean = (MBeanServer) this.resolver.evaluate(server, this.expressionContext);
			}
			else {
				bean = this.beanFactory.getBean(server, MBeanServer.class);
			}
			exporter.setServer(bean);
		}
	}

	private void setupComponentNamePatterns(IntegrationMBeanExporter exporter) {
		List<String> patterns = new ArrayList<>();
		String[] managedComponents = this.attributes.getStringArray("managedComponents");
		for (String managedComponent : managedComponents) {
			String pattern = this.environment.resolvePlaceholders(managedComponent);
			patterns.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(pattern)));
		}
		exporter.setComponentNamePatterns(patterns.toArray(new String[0]));
	}

}
