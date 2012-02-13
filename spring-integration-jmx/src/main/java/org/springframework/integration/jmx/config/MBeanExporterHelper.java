/*
 * Copyright 2002-2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.jmx.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.StringUtils;

/**
 * Most likely a temporary class mainly needed to address issue described in INT-2307.
 * It helps in eliminating conflicts when more than one MBeanExporter is present. It creates a list 
 * of bean names that will be exported by the IntegrationMBeanExporter and merges it with the list 
 * of 'excludedBeans' of MBeanExporter so it will not attempt to export them again.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
class MBeanExporterHelper implements BeanFactoryPostProcessor, BeanPostProcessor, Ordered {

	private final static String EXCLUDED_BEANS_PROPERTY_NAME = "excludedBeans";
	
	private final static String SI_ROOT_PACKAGE = "org.springframework.integration.";

	private final Set<String> siBeanNames = new HashSet<String>();
	
	@SuppressWarnings("unchecked")
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof MBeanExporter && !(bean instanceof IntegrationMBeanExporter)){
			MBeanExporter mbeanExporter = (MBeanExporter) bean;
			DirectFieldAccessor mbeDfa = new DirectFieldAccessor(mbeanExporter);
			Set<String> excludedNames = (Set<String>) mbeDfa.getPropertyValue(EXCLUDED_BEANS_PROPERTY_NAME);
			if (excludedNames != null) {
				siBeanNames.addAll(excludedNames);
			}
			mbeDfa.setPropertyValue(EXCLUDED_BEANS_PROPERTY_NAME, siBeanNames);
		}
		return bean;
	}

	
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String[] beanDefinitionNames = beanFactory.getBeanDefinitionNames();
		for (String beanName : beanDefinitionNames) {
			BeanDefinition bd = beanFactory.getMergedBeanDefinition(beanName);
			
			String className = bd.getBeanClassName();
			if (StringUtils.hasText(className)){
				if (className.startsWith(SI_ROOT_PACKAGE) && !(className.endsWith(IntegrationMBeanExporter.class.getName()))){
					siBeanNames.add(beanName);
				}
			}
		}
	}

	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE - 100;
	}
}
