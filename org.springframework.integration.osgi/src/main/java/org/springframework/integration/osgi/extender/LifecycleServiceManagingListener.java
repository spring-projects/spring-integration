/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.osgi.extender;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.Lifecycle;
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;

/**
 * Will start/stop {@link Lifecycle} beans that are dependent on OSGi Service references that are being bound/unbound
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class LifecycleServiceManagingListener implements OsgiServiceLifecycleListener, BeanFactoryAware {
	private static final Log log = LogFactory.getLog(LifecycleServiceManagingListener.class);
	private List<String> dependentSources;
	private BeanFactory beanFactory;

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#bind(java.lang.Object, java.util.Map)
	 */
	public void bind(Object service, Map properties) throws Exception {
		for (String dependentSource : dependentSources) {
			Object bean = beanFactory.getBean(dependentSource);
			if (bean instanceof Lifecycle){
				Lifecycle lifecycle = (Lifecycle) bean;
				if (!lifecycle.isRunning()){
					log.debug("Staring: " + lifecycle);
					lifecycle.start();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#unbind(java.lang.Object, java.util.Map)
	 */
	public void unbind(Object service, Map properties) throws Exception {
		for (String dependentSource : dependentSources) {
			Object bean = beanFactory.getBean(dependentSource);
			if (bean instanceof Lifecycle){
				Lifecycle lifecycle = (Lifecycle) bean;
				if (lifecycle.isRunning()){
					log.debug("Stopping: " + lifecycle);
					//lifecycle.stop();
					DirectFieldAccessor lifecycleAccessor = new DirectFieldAccessor(lifecycle);
					lifecycleAccessor.setPropertyValue("running", false);
				}
			}
		}
	}
	public List<String> getDependentSources() {
		return dependentSources;
	}

	public void setDependentSources(List<String> dependentSources) {
		this.dependentSources = dependentSources;
	}

	
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}
