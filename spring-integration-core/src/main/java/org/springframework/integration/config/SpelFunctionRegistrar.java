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

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>
 * Utility class that keeps track of a Map of SpEL functions in order to register
 * them with the "integrationEvaluationContext" upon initialization.
 * </p>
 * <p>
 * Seeing the {@link org.springframework.integration.config.xml.SpelFunctionParser}
 * doesn't register a bean for &lt;spel-function&gt; within application context,
 * then there is no automatic inherit way to get 'functions' from parent context
 * and this class provide a hook to get 'functions' from parent context.
 * </p>
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
class SpelFunctionRegistrar implements ApplicationContextAware, InitializingBean {

	private final Map<String, Method> functions;

	private ApplicationContext applicationContext;

	SpelFunctionRegistrar(Map<String, Method> functions) {
		this.functions = functions;
	}

	Map<String, Method> getFunctions() {
		return functions;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		ApplicationContext parent = this.applicationContext.getParent();
		if (parent != null) {
			try {
				SpelFunctionRegistrar parentFunctionRegistrar = parent.getBean(SpelFunctionRegistrar.class);
				Map<String, Method> parentFunctions = parentFunctionRegistrar.getFunctions();
				for (String key : parentFunctions.keySet()) {
					if(!this.functions.containsKey(key)) {
						this.functions.put(key, parentFunctions.get(key));
					}
				}
			}
			catch (NoSuchBeanDefinitionException e) {
				//Ignore it.
				//There is no <spel-function> components within parent application context.
			}
		}
	}

}
