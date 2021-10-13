/*
 * Copyright 2015-2020 the original author or authors.
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

import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportAware;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.util.Assert;

/**
 * {@code @Configuration} class that registers a {@link IntegrationManagementConfigurer} bean.
 *
 * <p>This configuration class is automatically imported when using the
 * {@link EnableIntegrationManagement} annotation. See its javadoc for complete usage details.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
@Configuration(proxyBeanMethods = false)
public class IntegrationManagementConfiguration implements ImportAware, EnvironmentAware {

	private AnnotationAttributes attributes;

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, Object> map = importMetadata.getAnnotationAttributes(EnableIntegrationManagement.class.getName());
		this.attributes = AnnotationAttributes.fromMap(map);
		Assert.notNull(this.attributes, () ->
				"@EnableIntegrationManagement is not present on importing class " + importMetadata.getClassName());
	}

	@Bean(name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public IntegrationManagementConfigurer managementConfigurer(ObjectProvider<MetricsCaptor> metricsCaptorProvider) {
		IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
		configurer.setDefaultLoggingEnabled(
				Boolean.parseBoolean(this.environment.resolvePlaceholders(
						(String) this.attributes.get("defaultLoggingEnabled"))));
		configurer.setMetricsCaptorProvider(metricsCaptorProvider);
		return configurer;
	}

}
