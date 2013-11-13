/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.util.ClassUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} support implementation, which registers
 * bean definitions for components from Core module.
 *
 * @author Artem Bilan
 * @since 3.0
 */
public class CoreIntegrationRegistrar implements ImportBeanDefinitionRegistrar, BeanClassLoaderAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private ClassLoader classLoader;

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		this.registerJsonPathSpelFunction(registry);
	}

	/**
	 * Registers '#jsonPath' SpEL function, if jayway json-path.jar is presented in the classpath.
	 */
	private void registerJsonPathSpelFunction(BeanDefinitionRegistry registry) {
		Class<?> jsonPathClass = null;
		try {
			jsonPathClass = ClassUtils.forName("com.jayway.jsonpath.JsonPath", this.classLoader);
		}
		catch (ClassNotFoundException e) {
			logger.debug("SpEL function '#jsonPath' isn't registered: there is no jayway json-path.jar on the classpath.");
		}

		if (jsonPathClass != null) {
			IntegrationNamespaceUtils.registerSpelFunctionBean(registry, "jsonPath", JsonPathUtils.class.getName(), "evaluate");
		}
	}

}
