/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.graph.IntegrationGraphServer;
import org.springframework.integration.http.management.IntegrationGraphController;

/**
 * Registers the necessary beans for {@link EnableIntegrationGraphController}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.3
 */
public class IntegrationGraphControllerRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log LOGGER = LogFactory.getLog(IntegrationGraphControllerRegistrar.class);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!HttpContextUtils.WEB_MVC_PRESENT && !HttpContextUtils.WEB_FLUX_PRESENT) {
			LOGGER.warn("The 'IntegrationGraphController' isn't registered with the application context because" +
					" there is no 'org.springframework.web.servlet.DispatcherServlet' or" +
					" 'org.springframework.web.reactive.DispatcherHandler' in the classpath.");
			return;
		}

		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnableIntegrationGraphController.class.getName());
		if (annotationAttributes == null) {
			annotationAttributes = Collections.emptyMap(); // To satisfy sonar for subsequent references
		}

		if (!registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME,
					new RootBeanDefinition(IntegrationGraphServer.class));
		}

		String path = (String) annotationAttributes.get("value");
		String[] allowedOrigins = (String[]) annotationAttributes.get("allowedOrigins");
		if (allowedOrigins != null && allowedOrigins.length > 0) {
			registerControllerCorsConfigurer(registry, path, allowedOrigins);
		}

		if (!registry.containsBeanDefinition(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME)) {
			registerIntegrationGraphController(registry, (String) annotationAttributes.get(AnnotationUtils.VALUE));
		}
	}

	private static void registerIntegrationGraphController(BeanDefinitionRegistry registry,
			String graphControllerPath) {

		AbstractBeanDefinition controllerPropertiesPopulator =
				BeanDefinitionBuilder.genericBeanDefinition(GraphControllerPropertiesPopulator.class)
						.addConstructorArgValue(graphControllerPath)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
						.getBeanDefinition();
		BeanDefinitionReaderUtils.registerWithGeneratedName(controllerPropertiesPopulator, registry);

		BeanDefinition graphController =
				BeanDefinitionBuilder.rootBeanDefinition(IntegrationGraphController.class)
						.addConstructorArgReference(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME)
						.getBeanDefinition();

		registry.registerBeanDefinition(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME, graphController);
	}

	private static void registerControllerCorsConfigurer(BeanDefinitionRegistry registry, String path,
			String[] allowedOrigins) {

		AbstractBeanDefinition controllerCorsConfigurer = null;
		if (HttpContextUtils.WEB_MVC_PRESENT) {
			controllerCorsConfigurer = webMvcControllerCorsConfigurerBean(path, allowedOrigins);
		}
		else if (HttpContextUtils.WEB_FLUX_PRESENT) {
			controllerCorsConfigurer = webFluxControllerCorsConfigurerBean(path, allowedOrigins);
		}

		if (controllerCorsConfigurer != null) {
			BeanDefinitionReaderUtils.registerWithGeneratedName(controllerCorsConfigurer, registry);
		}
		else {
			LOGGER.warn("Nor Spring MVC, neither WebFlux is present to configure CORS origins " +
					"for Integration Graph Controller.");
		}
	}

	private static AbstractBeanDefinition webMvcControllerCorsConfigurerBean(String path, String[] allowedOrigins) {
		return BeanDefinitionBuilder.genericBeanDefinition(WebMvcIntegrationGraphCorsConfigurer.class)
				.addConstructorArgValue(path)
				.addConstructorArgValue(allowedOrigins)
				.getBeanDefinition();
	}

	private static AbstractBeanDefinition webFluxControllerCorsConfigurerBean(String path, String[] allowedOrigins) {
		return BeanDefinitionBuilder.genericBeanDefinition(WebFluxIntegrationGraphCorsConfigurer.class)
				.addConstructorArgValue(path)
				.addConstructorArgValue(allowedOrigins)
				.getBeanDefinition();
	}

	static final class GraphControllerPropertiesPopulator
			implements BeanFactoryPostProcessor, EnvironmentAware {

		private final Map<String, Object> properties = new HashMap<>();

		GraphControllerPropertiesPopulator(String graphControllerPath) {
			this.properties.put(HttpContextUtils.GRAPH_CONTROLLER_PATH_PROPERTY, graphControllerPath);
		}

		@Override
		public void setEnvironment(Environment environment) {
			((ConfigurableEnvironment) environment)
					.getPropertySources()
					.addLast(new MapPropertySource(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME + "_properties",
							this.properties));
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

	}

}
