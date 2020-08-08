/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.MBeanExportConfiguration.SpecificPlatform;
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
public class IntegrationMBeanExportConfiguration implements ImportAware, EnvironmentAware, BeanFactoryAware {

	/**
	 * A name for default {@link IntegrationMBeanExporter} bean.
	 */
	public static final String MBEAN_EXPORTER_NAME = "integrationMbeanExporter";

	private AnnotationAttributes attributes;

	private BeanFactory beanFactory;

	private BeanExpressionResolver resolver = new StandardBeanExpressionResolver();

	private BeanExpressionContext expressionContext;

	private Environment environment;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.resolver = ((ConfigurableListableBeanFactory) beanFactory).getBeanExpressionResolver();
			this.expressionContext = new BeanExpressionContext((ConfigurableListableBeanFactory) beanFactory, null);
		}
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableIntegrationMBeanExport.class.getName());
		this.attributes = AnnotationAttributes.fromMap(map);
		Assert.notNull(this.attributes, () ->
				"@EnableIntegrationMBeanExport is not present on importing class " + importMetadata.getClassName());
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
		if (this.environment != null) {
			defaultDomain = this.environment.resolvePlaceholders(defaultDomain);
		}
		if (StringUtils.hasText(defaultDomain)) {
			exporter.setDefaultDomain(defaultDomain);
		}
	}

	private void setupServer(IntegrationMBeanExporter exporter) {
		String server = this.attributes.getString("server");
		if (this.environment != null) {
			server = this.environment.resolvePlaceholders(server);
		}
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
		else {
			SpecificPlatform specificPlatform = SpecificPlatform.get();
			if (specificPlatform != null) {
				exporter.setServer(specificPlatform.getMBeanServer());
			}
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
