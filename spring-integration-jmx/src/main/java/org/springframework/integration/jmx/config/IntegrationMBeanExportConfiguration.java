/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.jmx.config;

import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@code @Configuration} class that registers a {@link IntegrationMBeanExporter} bean.
 * <p/>
 * <p>This configuration class is automatically imported when using the
 * {@link EnableIntegrationMBeanExport} annotation. See its javadoc for complete usage details.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@Configuration
public class IntegrationMBeanExportConfiguration implements ImportAware, EnvironmentAware, BeanFactoryAware {

	private static final String MBEAN_EXPORTER_NAME = "mbeanExporter";

	private AnnotationAttributes attributes;

	private BeanFactory beanFactory;

	private Environment environment;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableIntegrationMBeanExport.class.getName());
		this.attributes = AnnotationAttributes.fromMap(map);
		Assert.notNull(this.attributes,
				"@EnableIntegrationMBeanExport is not present on importing class " + importMetadata.getClassName());
	}

	@Bean(name = MBEAN_EXPORTER_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public IntegrationMBeanExporter mbeanExporter() {
		IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
		setupDomain(exporter);
		setupServer(exporter);
		exporter.setRegistrationPolicy(this.attributes.<RegistrationPolicy>getEnum("registration"));
		exporter.setComponentNamePatterns(this.attributes.getStringArray("managedComponents"));
		return exporter;
	}

	private void setupDomain(IntegrationMBeanExporter exporter) {
		String defaultDomain = this.attributes.getString("defaultDomain");
		if (defaultDomain != null && this.environment != null) {
			defaultDomain = this.environment.resolvePlaceholders(defaultDomain);
		}
		if (StringUtils.hasText(defaultDomain)) {
			exporter.setDefaultDomain(defaultDomain);
		}
	}

	private void setupServer(IntegrationMBeanExporter exporter) {
		String server = this.attributes.getString("server");
		if (server != null && this.environment != null) {
			server = this.environment.resolvePlaceholders(server);
		}
		if (StringUtils.hasText(server)) {
			exporter.setServer(this.beanFactory.getBean(server, MBeanServer.class));
		}
		else {
			exporter.setServer(MBeanServerFactory.createMBeanServer());
		}
	}

}
