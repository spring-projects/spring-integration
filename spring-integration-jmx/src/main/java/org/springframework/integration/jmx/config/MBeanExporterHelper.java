/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.MBeanExporter;

/**
 * Most likely a temporary class mainly needed to address issue described in INT-2307.
 * It helps in eliminating conflicts when more than one MBeanExporter is present. It creates a list
 * of bean names that will be exported by the IntegrationMBeanExporter and merges it with the list
 * of 'excludedBeans' of MBeanExporter so it will not attempt to export them again.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 *
 */
class MBeanExporterHelper implements BeanPostProcessor, Ordered {

	private final List<MBeanExporter> mBeanExportersForExcludes = new ArrayList<MBeanExporter>();

	private final Set<String> siBeanNames = new HashSet<String>();

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if ("$autoCreateChannelCandidates".equals(beanName)) {
			@SuppressWarnings("unchecked")
			Collection<String> autoCreateChannelCandidatesNames = (Collection<String>) new DirectFieldAccessor(bean)
							.getPropertyValue("channelNames");
			this.siBeanNames.addAll(autoCreateChannelCandidatesNames);
			if (!this.mBeanExportersForExcludes.isEmpty()) {
				for (String autoCreateChannelCandidatesName : autoCreateChannelCandidatesNames) {
					for (MBeanExporter mBeanExporter : this.mBeanExportersForExcludes) {
						mBeanExporter.addExcludedBean(autoCreateChannelCandidatesName);
					}
				}
			}
		}

		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (AnnotatedElementUtils.isAnnotated(AopUtils.getTargetClass(bean),
				IntegrationManagedResource.class.getName())) {
			this.siBeanNames.add(beanName);
			if (!this.mBeanExportersForExcludes.isEmpty()) {
				for (MBeanExporter mBeanExporter : this.mBeanExportersForExcludes) {
					mBeanExporter.addExcludedBean(beanName);
				}
			}
		}

		if (bean instanceof MBeanExporter && !(bean instanceof IntegrationMBeanExporter)) {
			MBeanExporter mBeanExporter = (MBeanExporter) bean;
			this.mBeanExportersForExcludes.add(mBeanExporter);
			for (String siBeanName : this.siBeanNames) {
				mBeanExporter.addExcludedBean(siBeanName);
			}
		}

		return bean;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

}
