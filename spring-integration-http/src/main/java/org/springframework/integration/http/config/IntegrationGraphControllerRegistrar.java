/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.HashMap;
import java.util.Map;

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
import org.springframework.integration.http.management.IntegrationGraphController;
import org.springframework.integration.http.support.HttpContextUtils;
import org.springframework.integration.support.management.graph.IntegrationGraphServer;

/**
 * @author Artem Bilan
 * @since 4.3
 */
class IntegrationGraphControllerRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnableIntegrationGraphController.class.getName());

		if (!registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME)) {
			registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME,
					new RootBeanDefinition(IntegrationGraphServer.class));
		}

		if (!registry.containsBeanDefinition(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME)) {
			AbstractBeanDefinition controllerPropertiesPopulator =
					BeanDefinitionBuilder.genericBeanDefinition(GraphControllerPropertiesPopulator.class)
							.addConstructorArgValue(annotationAttributes)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
							.getBeanDefinition();
			BeanDefinitionReaderUtils.registerWithGeneratedName(controllerPropertiesPopulator, registry);

			BeanDefinition graphController =
					BeanDefinitionBuilder.genericBeanDefinition(IntegrationGraphController.class)
							.addConstructorArgReference(IntegrationContextUtils.INTEGRATION_GRAPH_SERVER_BEAN_NAME)
							.getBeanDefinition();

			registry.registerBeanDefinition(HttpContextUtils.GRAPH_CONTROLLER_BEAN_NAME, graphController);
		}
	}

	private static final class GraphControllerPropertiesPopulator
			implements BeanFactoryPostProcessor, EnvironmentAware {

		private final Map<String, Object> properties = new HashMap<String, Object>();

		private GraphControllerPropertiesPopulator(Map<String, Object> annotationAttributes) {
			Object graphControllerPath = annotationAttributes.get(AnnotationUtils.VALUE);
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
