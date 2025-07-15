/*
 * Copyright 2015-present the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

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
import org.springframework.integration.support.management.ControlBusCommandRegistry;
import org.springframework.integration.support.management.metrics.MetricsCaptor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
public class IntegrationManagementConfiguration implements ImportAware, EnvironmentAware {

	private final ControlBusCommandRegistry controlBusCommandRegistry;

	private @Nullable AnnotationAttributes attributes;

	@SuppressWarnings("NullAway.Init")
	private Environment environment;

	public IntegrationManagementConfiguration(ControlBusCommandRegistry controlBusCommandRegistry) {
		this.controlBusCommandRegistry = controlBusCommandRegistry;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		Map<String, @Nullable Object> map = Objects.requireNonNull(importMetadata.getAnnotationAttributes(EnableIntegrationManagement.class.getName()));
		this.attributes = AnnotationAttributes.fromMap(map);
		Assert.notNull(this.attributes, () ->
				"@EnableIntegrationManagement is not present on importing class " + importMetadata.getClassName());
		this.controlBusCommandRegistry.setEagerInitialization(
				Boolean.parseBoolean(
						this.environment.resolvePlaceholders(this.attributes.getString("loadControlBusCommands"))));
	}

	@Bean(name = IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME)
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public IntegrationManagementConfigurer managementConfigurer(
			ObjectProvider<MetricsCaptor> metricsCaptorProvider,
			ObjectProvider<ObservationRegistry> observationRegistryProvider) {

		IntegrationManagementConfigurer configurer = new IntegrationManagementConfigurer();
		String defaultLoggingEnabled = (String) Objects.requireNonNull(this.attributes).get("defaultLoggingEnabled");
		configurer.setDefaultLoggingEnabled(
				Boolean.parseBoolean(this.environment.resolvePlaceholders(defaultLoggingEnabled == null
						? "false" : defaultLoggingEnabled)));
		configurer.setMetricsCaptorProvider(metricsCaptorProvider);
		String[] observationPatterns = obtainObservationPatterns();
		if (observationPatterns.length > 0) {
			configurer.setObservationPatterns(observationPatterns);
			configurer.setObservationRegistry(observationRegistryProvider);
		}
		return configurer;
	}

	private String[] obtainObservationPatterns() {
		Collection<String> observationPatterns = new HashSet<>();
		String[] patternsProperties = (String[]) Objects.requireNonNull(this.attributes).get("observationPatterns");
		patternsProperties = patternsProperties == null ? new String[0] : patternsProperties;
		boolean hasAsterisk = false;
		for (String patternProperty : patternsProperties) {
			String patternValue = this.environment.resolvePlaceholders(patternProperty);
			String[] patternsToProcess = StringUtils.commaDelimitedListToStringArray(patternValue);
			for (String pattern : patternsToProcess) {
				hasAsterisk |= "*".equals(pattern);
				if (StringUtils.hasText(pattern) && (pattern.startsWith("!") || !hasAsterisk)) {
					observationPatterns.add(pattern);
				}
			}
		}
		if (hasAsterisk) {
			observationPatterns =
					observationPatterns.stream()
							.filter((pattern) -> pattern.startsWith("!"))
							.collect(Collectors.toList());

			observationPatterns.add("*");
		}
		return observationPatterns.toArray(new String[0]);
	}

}
