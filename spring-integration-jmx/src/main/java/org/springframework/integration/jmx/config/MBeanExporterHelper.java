/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Most likely a temporary class mainly needed to address issue described in INT-2307.
 * It helps in eliminating conflicts when more than one MBeanExporter is present. It creates a list
 * of bean names that will be exported by the IntegrationMBeanExporter and merges it with the list
 * of 'excludedBeans' of MBeanExporter so it will not attempt to export them again.
 *
 * Since 4.2, we unconditionally exclude all channels from standard context exporter (channels
 * are now managed resources and would otherwise be picked up).
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 *
 */
class MBeanExporterHelper implements BeanPostProcessor, Ordered, BeanFactoryAware, InitializingBean {

	private final static String EXCLUDED_BEANS_PROPERTY_NAME = "excludedBeans";

	private final static String SI_ROOT_PACKAGE = "org.springframework.integration.";

	private final Set<String> siBeanNames = new HashSet<String>();

	/**
	 * For backwards compatibility, we don't want a context MBeanExporter to export
	 * integration components that are now managed resources. When there *is* an IMBE,
	 * *all* managed resources in org.springframework.integration are exluded.
	 */
	private final Set<String> siBeansToExcludeWhenNoIMBE = new HashSet<String>();

	private volatile DefaultListableBeanFactory beanFactory;

	private volatile boolean capturedAutoChannelCandidates;

	private volatile boolean hasIntegrationExporter;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(DefaultListableBeanFactory.class, beanFactory);
		this.beanFactory = (DefaultListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.beanFactory != null) {
			String[] beanNames = this.beanFactory.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				BeanDefinition def = this.beanFactory.getBeanDefinition(beanName);
				String className = def.getBeanClassName();
				if (className == null && def.getSource() instanceof StandardMethodMetadata) {
					className = ((StandardMethodMetadata) def.getSource()).getIntrospectedMethod().getReturnType().getName();
				}
				if (StringUtils.hasText(className)){
					if (className.startsWith(SI_ROOT_PACKAGE)
							&& !(className.endsWith(IntegrationMBeanExporter.class.getName()))){
						this.siBeanNames.add(beanName);
					}
					if (className.startsWith(SI_ROOT_PACKAGE + "channel.")
							&& !(className.endsWith(IntegrationMBeanExporter.class.getName()))){
						this.siBeansToExcludeWhenNoIMBE.add(beanName);
					}
					else if (className.equals(IntegrationMBeanExporter.class.getName())) {
						this.hasIntegrationExporter = true;
					}
				}
			}
		}
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (!this.capturedAutoChannelCandidates && this.beanFactory != null) {
			Object autoCreateChannelCandidates = beanFactory.getBean("$autoCreateChannelCandidates");
			if (autoCreateChannelCandidates != null){
					@SuppressWarnings("unchecked")
					Collection<String> autoCreateChannelCandidatesNames =
									(Collection<String>) new DirectFieldAccessor(autoCreateChannelCandidates).getPropertyValue("channelNames");
					this.siBeanNames.addAll(autoCreateChannelCandidatesNames);
					this.siBeansToExcludeWhenNoIMBE.addAll(autoCreateChannelCandidatesNames);
			}
			this.capturedAutoChannelCandidates = true;
		}
		if (bean instanceof MBeanExporter && !(bean instanceof IntegrationMBeanExporter)) {
			MBeanExporter mbeanExporter = (MBeanExporter) bean;
			DirectFieldAccessor mbeDfa = new DirectFieldAccessor(mbeanExporter);
			@SuppressWarnings("unchecked")
			Set<String> excludedNames = (Set<String>) mbeDfa.getPropertyValue(EXCLUDED_BEANS_PROPERTY_NAME);
			if (excludedNames != null) {
				excludedNames = new HashSet<String>(excludedNames);
			}
			else {
				excludedNames = new HashSet<String>();
			}
			if (!this.hasIntegrationExporter) {
				excludedNames.addAll(this.siBeansToExcludeWhenNoIMBE);
			}
			else {
				excludedNames.addAll(this.siBeanNames);
			}
			//TODO: SF 4.1.5 and above now has additive exclusions (SPR-12686)
			mbeDfa.setPropertyValue(EXCLUDED_BEANS_PROPERTY_NAME, excludedNames);
		}

		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
