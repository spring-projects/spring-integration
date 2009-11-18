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

import org.springframework.context.Lifecycle;
import org.springframework.osgi.service.importer.OsgiServiceLifecycleListener;

/**
 * TODO - insert COMMENT
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class IntegrationServiceBindingListener implements
		OsgiServiceLifecycleListener {
	private List<?> dependentSources;

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#bind(java.lang.Object, java.util.Map)
	 */
	public void bind(Object service, Map properties) throws Exception {
		for (Object dependentSource : dependentSources) {
			if (dependentSource instanceof Lifecycle){
				Lifecycle lifecycle = (Lifecycle) dependentSource;
				if (lifecycle.isRunning()){
					lifecycle.stop();
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.osgi.service.importer.OsgiServiceLifecycleListener#unbind(java.lang.Object, java.util.Map)
	 */
	public void unbind(Object service, Map properties) throws Exception {
		for (Object dependentSource : dependentSources) {
			if (dependentSource instanceof Lifecycle){
				Lifecycle lifecycle = (Lifecycle) dependentSource;
				if (!lifecycle.isRunning()){
					lifecycle.start();
				}
			}
		}
	}
	public List getDependentSources() {
		return dependentSources;
	}

	public void setDependentSources(List dependentSources) {
		this.dependentSources = dependentSources;
	}
}
